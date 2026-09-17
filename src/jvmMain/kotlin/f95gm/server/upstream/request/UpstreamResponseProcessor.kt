package f95gm.server.upstream.request

import f95gm.server.observability.*

import f95gm.server.observability.UpstreamResponseEvent
import f95gm.server.observability.logger
import f95gm.server.observability.structuredLog
import f95gm.server.http.logging.redactLogValue
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.model.http.UpstreamResponse.Success
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod

internal class UpstreamResponseProcessor(
    private val responseReader: UpstreamResponseReader,
    private val circuitActions: UpstreamFailureRecorder,
    private val metrics: UpstreamMetrics
) {
    suspend fun process(
        method: HttpMethod,
        circuitKey: UpstreamCircuitKey,
        response: io.ktor.client.statement.HttpResponse,
        session: UpstreamSession,
        policy: UpstreamPolicy,
        retry: RetryOutcome.Response,
        timing: UpstreamRequestTiming
    ): UpstreamResponse {
        val upstreamResponse = when (val parsed = responseReader.read(response, session, UpstreamResponsePolicy(policy.maxResponseBytes))) {
            is Success -> parsed.copy(retryAttempts = retry.attempts, retryReason = retry.reason)
            is UpstreamResponse.Failure -> parsed
        }
        val outcome = UpstreamOutcomePolicy.decide(
            response = upstreamResponse,
            upstreamStatus = response.status,
            retryAttempts = retry.attempts,
            retryReason = retry.reason
        )
        val duration = timing.elapsed()
        circuitActions.applyCircuitAction(circuitKey, outcome.circuitAction)
        logger.info {
            structuredLog(
                UpstreamResponseEvent(
                    method = method.value,
                    uri = redactLogValue(circuitKey.value),
                    status = response.status.value,
                    contentLength = response.headers[HttpHeaders.ContentLength] ?: "unknown",
                    responseBytes = upstreamResponse.bodyByteCount,
                    retryAttempts = RetryAttempt(retry.attempts),
                    durationMs = RequestDurationMillis(duration.inWholeMilliseconds)
                )
            )
        }
        metrics.record(
            endpoint = UpstreamMetricEndpoint(circuitKey.value),
            status = UpstreamMetricStatus(response.status.value),
            retries = retry.attempts,
            bytes = upstreamResponse.bodyByteCount,
            duration = duration,
            outcome = outcome.metricOutcome,
            retryReason = outcome.retryReason
        )
        return outcome.response
    }
}
