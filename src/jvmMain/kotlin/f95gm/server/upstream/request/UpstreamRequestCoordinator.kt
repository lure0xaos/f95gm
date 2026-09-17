package f95gm.server.upstream.request

import f95gm.server.config.constants.HttpStatusCatalog
import f95gm.server.observability.UpstreamCircuitOpenEvent
import f95gm.server.observability.UpstreamMetricEndpoint
import f95gm.server.observability.UpstreamMetricOutcome
import f95gm.server.observability.UpstreamMetricStatus
import f95gm.server.observability.UpstreamMetrics
import f95gm.server.observability.UpstreamRetryBudgetExhaustedEvent
import f95gm.server.observability.UpstreamTransportFailureEvent
import f95gm.server.observability.RetryAttempt
import f95gm.server.observability.logger
import f95gm.server.observability.structuredLog
import f95gm.server.http.logging.redactLogValue
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.model.http.UpstreamResponse.Success
import f95gm.server.model.http.ResponseBodyBytes
import f95gm.server.model.http.upstreamCircuitOpenResponse
import f95gm.server.kernel.ServerClock
import f95gm.server.kernel.RetryExecutor
import f95gm.server.kernel.RetryFailureReason
import f95gm.server.kernel.SingleFlight
import f95gm.server.state.runtime.session.SessionLockRegistry
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import java.io.IOException
import f95gm.server.kernel.ServerInvariantViolation

internal class UpstreamRequestCoordinator(
    private val transport: UpstreamTransport,
    private val responseReader: UpstreamResponseReader,
    sessionLocks: SessionLockRegistry,
    getFlights: SingleFlight<UpstreamFlightKey, UpstreamResponse>,
    private val circuitBreaker: UpstreamCircuitBreaker,
    private val defaultPolicy: UpstreamPolicy,
    private val clock: ServerClock,
    private val upstreamMetrics: UpstreamMetrics,
    maxConcurrentRequests: Int = UpstreamConcurrencyDispatcher.DEFAULT_MAX_CONCURRENT_REQUESTS
) {
    private val attemptExecutor = UpstreamRequestAttemptExecutor(transport)
    private val circuitGate = UpstreamCircuitGate(circuitBreaker)
    private val concurrency = UpstreamConcurrencyDispatcher(sessionLocks, getFlights, maxConcurrentRequests)
    private val failureRecorder = UpstreamFailureRecorder(circuitBreaker, upstreamMetrics)
    private val responseProcessor = UpstreamResponseProcessor(responseReader, failureRecorder, upstreamMetrics)

    suspend fun request(
        method: HttpMethod,
        path: UpstreamPath,
        context: UpstreamRequestContext
    ): UpstreamResponse = concurrency.dispatch(
        method, path, context.session, context.body, context.contentType, context.headers
    ) { requestUnlocked(method, path, context, context.policy.resolve(defaultPolicy)) }

    @Suppress("ThrowsCount") // Distinguish retry-budget invariant failures from transport and caller cancellation.
    private suspend fun requestUnlocked(
        method: HttpMethod,
        path: UpstreamPath,
        context: UpstreamRequestContext,
        policy: UpstreamPolicy
    ): UpstreamResponse {
        val uri = transport.safeUri(path)
        val circuitKey = UpstreamCircuitKey(uri.value.substringBefore('?'))
        val timing = UpstreamRequestTiming.start(clock)
        if (circuitGate.evaluate(circuitKey) is UpstreamCircuitGate.Decision.Open) {
            logger.warn { structuredLog(UpstreamCircuitOpenEvent(redactLogValue(circuitKey.value))) }
            val outcome = UpstreamOutcomePolicy.special(upstreamCircuitOpenResponse(), UpstreamMetricOutcome.CIRCUIT_OPEN)
            upstreamMetrics.record(UpstreamMetricEndpoint(circuitKey.value), UpstreamMetricStatus(503), outcome.retryAttempts, ResponseBodyBytes(0), timing.elapsed(), outcome.metricOutcome, outcome.retryReason)
            return outcome.response
        }
        val retried = try {
            attemptExecutor.execute(method, uri, policy, context)
        } catch (error: IOException) {
            return failureRecorder.transportFailure(circuitKey, timing, error)
        } catch (error: RetryExecutor) {
            if (error.reason != RetryFailureReason.DELAY_BUDGET_EXHAUSTED) throw error
            val deadlineBound = policy.maxTotalRetryDelay >= policy.requestTimeout
            upstreamMetrics.recordRetryBudgetExhaustion(deadlineBound)
            logger.warn {
                structuredLog(
                    UpstreamRetryBudgetExhaustedEvent(
                        method = method.value,
                        uri = redactLogValue(uri.value.substringBefore('?')),
                        reason = error.reason,
                        requestedDelay = error.requestedDelay,
                        remainingDelay = error.remainingDelay
                    )
                )
            }
            return failureRecorder.retryBudgetFailure(circuitKey, timing, deadlineBound, error)
        } catch (error: CancellationException) {
            upstreamMetrics.recordRetryCallerCancellation()
            throw error
        }
        if (retried is RetryOutcome.Failure) {
            logger.warn {
                structuredLog(
                    UpstreamTransportFailureEvent(
                        method.value,
                        redactLogValue(circuitKey.value),
                        RetryAttempt(retried.attempts),
                        retried.failure
                    )
                )
            }
            return failureRecorder.retryFailure(circuitKey, timing, retried)
        }
        val response = when (retried) {
            is RetryOutcome.Failure -> throw ServerInvariantViolation("Retry failure was handled above")
            is RetryOutcome.Response -> retried.response
        }
        return responseProcessor.process(method, circuitKey, response, context.session, policy, retried, timing)
    }

}
