package f95gm.server.upstream.request

import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.logging.*
import f95gm.server.http.logging.redactLogValue
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamFailure
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.model.http.UpstreamResponse.Success
import f95gm.server.model.http.upstreamCircuitOpenResponse
import f95gm.server.state.runtime.ServerClock
import f95gm.server.state.runtime.SingleFlight
import f95gm.server.state.runtime.session.SessionLockRegistry
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

internal class UpstreamRequestCoordinator(
    private val transport: UpstreamTransport,
    private val responseReader: UpstreamResponseReader,
    private val sessionLocks: SessionLockRegistry,
    private val getFlights: SingleFlight<String, UpstreamResponse>,
    private val circuitBreaker: UpstreamCircuitBreaker,
    private val policy: UpstreamPolicy,
    private val clock: ServerClock
) {
    suspend fun request(
        method: HttpMethod,
        path: String,
        session: UpstreamSession,
        body: String?,
        contentType: String?,
        headers: List<UpstreamHeader>
    ): UpstreamResponse = if (method == HttpMethod.Get) {
        val key = listOf(
            session.browserId,
            method.value,
            path,
            body.orEmpty(),
            contentType.orEmpty(),
            headers.associate { it.name to it.value }.toSortedMap()
        ).joinToString("|")
        getFlights.run(key) { requestUnlocked(method, path, session, body, contentType, headers) }
    } else {
        sessionLocks.withLock(session.browserId) {
            requestUnlocked(method, path, session, body, contentType, headers)
        }
    }

    private suspend fun requestUnlocked(
        method: HttpMethod,
        path: String,
        session: UpstreamSession,
        body: String?,
        contentType: String?,
        headers: List<UpstreamHeader>
    ): UpstreamResponse {
        val uri = transport.safeUri(path)
        val circuitKey = uri.substringBefore('?')
        val started = clock.monotonicNanos()
        if (!circuitBreaker.allow(circuitKey)) {
            logger.warn { structuredLog(UpstreamCircuitOpenEvent(redactLogValue(circuitKey))) }
            return upstreamCircuitOpenResponse()
        }
        val retried = try {
            withTimeout(policy.requestTimeout) {
                transport.requestWithRetry(method, uri) {
                    applyUpstreamDefaults(method, session, headers, contentType, body)
                }
            }
        } catch (error: IOException) {
            circuitBreaker.recordFailure(circuitKey)
            return UpstreamResponse.Failure(
                status = ServerConstants.STATUS_BAD_GATEWAY,
                body = "",
                contentType = null,
                reason = UpstreamFailure.TRANSPORT
            )
        } catch (error: kotlinx.coroutines.TimeoutCancellationException) {
            circuitBreaker.recordFailure(circuitKey)
            return UpstreamResponse.Failure(
                status = ServerConstants.STATUS_GATEWAY_TIMEOUT,
                body = "",
                contentType = null,
                reason = UpstreamFailure.TIMEOUT
            )
        } catch (error: CancellationException) {
            throw error
        }
        if (retried is RetryOutcome.Failure) {
            circuitBreaker.recordFailure(circuitKey)
            logger.warn {
                structuredLog(
                    UpstreamTransportFailureEvent(
                        method.value,
                        redactLogValue(circuitKey),
                        retried.attempts,
                        retried.failure.name
                    )
                )
            }
            return UpstreamResponse.Failure(
                status = ServerConstants.STATUS_BAD_GATEWAY,
                body = "",
                contentType = null,
                reason = retried.failure
            )
        }
        val response = (retried as RetryOutcome.Response).response
        if (response.status in retryableStatuses) circuitBreaker.recordFailure(circuitKey)
        else circuitBreaker.recordSuccess(circuitKey)
        val upstreamResponse = when (val parsed = responseReader.read(response, session)) {
            is Success -> parsed.copy(retryAttempts = retried.attempts, retryReason = retried.reason)
            is UpstreamResponse.Failure -> parsed
        }
        logger.info {
            structuredLog(
                UpstreamResponseEvent(
                    method = method.value,
                    uri = redactLogValue(circuitKey),
                    status = response.status.value,
                    contentLength = response.headers[HttpHeaders.ContentLength] ?: "unknown",
                    responseBytes = upstreamResponse.body.encodeToByteArray().size,
                    retryAttempts = retried.attempts,
                    durationMs = (clock.monotonicNanos() - started).coerceAtLeast(0L) / 1_000_000L
                )
            )
        }
        return upstreamResponse
    }
}
