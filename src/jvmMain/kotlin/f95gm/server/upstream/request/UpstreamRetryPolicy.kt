package f95gm.server.upstream.request

import f95gm.server.model.http.UpstreamRetryReason
import f95gm.server.model.EpochMillis
import f95gm.domain.api.RetryDelayPolicy
import f95gm.domain.api.RetryJitter
import f95gm.domain.api.parseRetryAfter
import io.ktor.http.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class UpstreamRetryPolicy(
    private val policy: UpstreamPolicy,
    private val jitter: RetryJitter
) {
    private val delayPolicy = RetryDelayPolicy(
        policy.retryBaseDelay,
        policy.retryMaxDelay,
        jitter
    )
    fun responseDecision(
        method: io.ktor.http.HttpMethod,
        status: HttpStatusCode,
        headers: io.ktor.http.Headers,
        attempt: Int,
        now: EpochMillis
    ): UpstreamRetryDecision? = if (shouldRetry(method, status, attempt)) {
        UpstreamRetryDecision.Retry(UpstreamRetryReason.HTTP_STATUS, httpDelay(headers, attempt, now))
    } else null

    fun transportDecision(method: io.ktor.http.HttpMethod, attempt: Int): UpstreamRetryDecision? =
        if (shouldRetryTransport(method, attempt)) {
            UpstreamRetryDecision.Retry(UpstreamRetryReason.IO_EXCEPTION, ioDelay(attempt))
        } else null

    fun shouldRetry(method: io.ktor.http.HttpMethod, status: HttpStatusCode, attempt: Int): Boolean =
        method == io.ktor.http.HttpMethod.Get && status in retryableStatuses && attempt < policy.maxAttempts - 1

    fun shouldRetryTransport(method: io.ktor.http.HttpMethod, attempt: Int): Boolean =
        method == io.ktor.http.HttpMethod.Get && attempt < policy.maxAttempts - 1

    fun httpDelay(headers: io.ktor.http.Headers, attempt: Int, now: EpochMillis): Duration =
        headers[HttpHeaders.RetryAfter]?.let { parseRetryAfter(it, now.value)?.duration }
            ?.let { delayPolicy.delay(attempt, it) }
            ?: delayPolicy.delay(attempt)

    fun ioDelay(attempt: Int): Duration = delayPolicy.delay(attempt)
}

internal sealed interface UpstreamRetryDecision {
    data class Retry(val reason: UpstreamRetryReason, val delay: Duration) : UpstreamRetryDecision
}
