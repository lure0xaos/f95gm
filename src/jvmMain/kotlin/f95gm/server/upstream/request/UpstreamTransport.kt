package f95gm.server.upstream.request

import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.logging.UpstreamRetryEvent
import f95gm.server.config.logging.logger
import f95gm.server.config.logging.structuredLog
import f95gm.server.config.upstream.upstream
import f95gm.server.http.logging.redactLogValue
import f95gm.server.http.request.RequestMetrics
import f95gm.server.http.request.SafeHttpsUri
import f95gm.server.http.request.SafeHttpsUriResult
import f95gm.server.model.http.UpstreamFailure
import f95gm.server.model.http.UpstreamRetryReason
import f95gm.server.state.runtime.RetryDecision
import f95gm.server.state.runtime.retrySuspending
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.IOException
import java.net.URI

internal class UpstreamTransport(
    private val httpClient: UpstreamHttpClient,
    private val policy: UpstreamPolicy,
    private val retryPolicy: UpstreamRetryPolicy,
    private val retryDelay: suspend (Long) -> Unit,
    private val metrics: RequestMetrics
) {
    fun safeUri(path: String): String {
        val base = URI(upstream)
        if (path.startsWith("/")) return upstream.trimEnd('/') + path
        if (base.scheme.equals("http", ignoreCase = true) && base.host.isLocalLoopback()) {
            val candidate = runCatching { URI(path) }.getOrNull()
            require(
                candidate != null && candidate.scheme.equals("http", ignoreCase = true) &&
                        candidate.host.equals(base.host, ignoreCase = true) && candidate.port == base.port &&
                        candidate.userInfo == null && candidate.fragment == null
            ) { "Upstream URI is not allowed in loopback fixture mode" }
            return candidate.toString()
        }
        return when (val result = SafeHttpsUri.parseResult(path, setOf(base.host), setOf(-1, base.port, 443))) {
            is SafeHttpsUriResult.Valid -> result.uri.toString()
            is SafeHttpsUriResult.Invalid -> error("Upstream URI is not allowed: ${result.name}")
        }
    }

    suspend fun requestWithRetry(
        method: HttpMethod,
        uri: String,
        block: HttpRequestBuilder.() -> Unit
    ): RetryOutcome {
        var lastRetryReason: UpstreamRetryReason? = null
        return retrySuspending(policy.maxAttempts, pause = retryDelay) { attempt ->
            try {
                val response = httpClient.request(uri, block)
                if (retryPolicy.shouldRetry(method, response.status, attempt)) {
                    lastRetryReason = UpstreamRetryReason.HTTP_STATUS
                    logger.warn {
                        structuredLog(
                            UpstreamRetryEvent(
                                method.value,
                                redactLogValue(uri.substringBefore('?')),
                                attempt + 1
                            )
                        )
                    }
                    metrics.recordRetry()
                    response.bodyAsChannel().cancel(null)
                    RetryDecision.Again(
                        f95gm.server.config.Millis(
                            retryPolicy.httpDelay(
                                response.headers,
                                attempt
                            ).inWholeMilliseconds
                        )
                    )
                } else {
                    RetryDecision.Return(RetryOutcome.Response(response, attempt + 1, lastRetryReason))
                }
            } catch (error: IOException) {
                if (method != HttpMethod.Get || attempt == policy.maxAttempts - 1) {
                    return@retrySuspending RetryDecision.Return(
                        RetryOutcome.Failure(
                            UpstreamFailure.TRANSPORT,
                            attempt + 1,
                            lastRetryReason ?: UpstreamRetryReason.IO_EXCEPTION
                        )
                    )
                }
                lastRetryReason = UpstreamRetryReason.IO_EXCEPTION
                metrics.recordRetry()
                RetryDecision.Again(f95gm.server.config.Millis(retryPolicy.ioDelay(attempt).inWholeMilliseconds))
            }
        }
    }
}

private fun String?.isLocalLoopback(): Boolean = this?.lowercase() in setOf("localhost", "127.0.0.1", "::1", "[::1]")

internal sealed interface RetryOutcome {
    val attempts: Int
    val reason: UpstreamRetryReason?

    data class Response(
        val response: HttpResponse,
        override val attempts: Int,
        override val reason: UpstreamRetryReason?
    ) : RetryOutcome

    data class Failure(
        val failure: UpstreamFailure,
        override val attempts: Int,
        override val reason: UpstreamRetryReason?
    ) : RetryOutcome
}

internal val retryableStatuses = setOf(
    ServerConstants.STATUS_BAD_GATEWAY,
    ServerConstants.STATUS_SERVICE_UNAVAILABLE,
    ServerConstants.STATUS_GATEWAY_TIMEOUT
)
