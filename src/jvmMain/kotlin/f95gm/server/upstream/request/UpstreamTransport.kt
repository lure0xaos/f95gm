package f95gm.server.upstream.request

import f95gm.server.config.constants.HttpStatusCatalog
import f95gm.server.config.upstream.UpstreamBaseUrl
import f95gm.server.observability.UpstreamRetryEvent
import f95gm.server.observability.RetryAttempt
import f95gm.server.observability.logger
import f95gm.server.observability.structuredLog
import f95gm.server.http.logging.redactLogValue
import f95gm.server.http.request.RequestMetrics
import f95gm.server.model.http.UpstreamRetryReason
import f95gm.server.model.http.UpstreamFailure
import f95gm.server.model.EpochMillis
import f95gm.server.kernel.RetryDecision
import f95gm.server.kernel.retrySuspending
import f95gm.server.kernel.ServerClock
import f95gm.domain.api.RetryJitter
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.IOException
import kotlin.time.Duration

internal class UpstreamTransport(
    private val httpClient: UpstreamHttpClient,
    private val clock: ServerClock,
    private val jitter: RetryJitter,
    private val retryDelay: suspend (Duration) -> Unit,
    private val metrics: RequestMetrics,
    private val upstreamBaseUrl: UpstreamBaseUrl
) {
    fun safeUri(path: UpstreamPath): UpstreamRequestUri {
        return UpstreamRequestUri(UpstreamUrlPolicy.safeRequest(path, upstreamBaseUrl))
    }

    suspend fun requestWithRetry(
        method: HttpMethod,
        uri: UpstreamRequestUri,
        policy: UpstreamPolicy,
        block: HttpRequestBuilder.() -> Unit
    ): RetryOutcome {
        var lastRetryReason: UpstreamRetryReason? = null
        val retryPolicy = UpstreamRetryPolicy(policy, jitter)
        return retrySuspending(
            policy.attemptLimit,
            pause = retryDelay,
            maxTotalDelay = policy.maxTotalRetryDelay.coerceAtMost(policy.requestTimeout)
        ) { attempt ->
            try {
                val response = httpClient.request(UpstreamUri(uri.value), block)
                val responseDecision = retryPolicy.responseDecision(
                    method, response.status, response.headers, attempt, clock.epochMillis()
                )
                if (responseDecision is UpstreamRetryDecision.Retry) {
                    lastRetryReason = responseDecision.reason
                    logger.warn {
                        structuredLog(
                            UpstreamRetryEvent(
                                method.value,
                                redactLogValue(uri.value.substringBefore('?')),
                                RetryAttempt(attempt + 1)
                            )
                        )
                    }
                    metrics.recordRetry()
                    response.releaseUnconsumedBody()
                    RetryDecision.Again(responseDecision.delay)
                } else {
                    RetryDecision.Return(RetryOutcome.Response(response, attempt + 1, lastRetryReason))
                }
            } catch (error: IOException) {
                val transportDecision = retryPolicy.transportDecision(method, attempt)
                if (transportDecision == null) {
                    return@retrySuspending RetryDecision.Return(
                        RetryOutcome.Failure(
                            UpstreamFailure.CONNECTION_FAILURE,
                            attempt + 1,
                            lastRetryReason ?: UpstreamRetryReason.IO_EXCEPTION
                        )
                    )
                }
                val decision = transportDecision as UpstreamRetryDecision.Retry
                lastRetryReason = decision.reason
                metrics.recordRetry()
                RetryDecision.Again(decision.delay)
            }
        }
    }
}

internal val retryableStatuses = setOf(
    HttpStatusCatalog.BAD_GATEWAY,
    HttpStatusCatalog.SERVICE_UNAVAILABLE,
    HttpStatusCatalog.GATEWAY_TIMEOUT
)
