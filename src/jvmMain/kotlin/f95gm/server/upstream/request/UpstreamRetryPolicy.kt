package f95gm.server.upstream.request

import f95gm.server.state.runtime.ServerClock
import io.ktor.http.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class UpstreamRetryPolicy(
    private val policy: UpstreamPolicy,
    private val clock: ServerClock,
    private val jitterMillis: (Int) -> Long
) {
    fun shouldRetry(method: io.ktor.http.HttpMethod, status: HttpStatusCode, attempt: Int): Boolean =
        method == io.ktor.http.HttpMethod.Get && status in retryableStatuses && attempt < policy.maxAttempts - 1

    fun httpDelay(headers: io.ktor.http.Headers, attempt: Int): Duration =
        headers[HttpHeaders.RetryAfter]?.let { retryAfter ->
            retryAfter.toLongOrNull()?.milliseconds?.times(1_000)
                ?: try {
                    val target = ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                    (target.toEpochMilli() - clock.nowMillis()).coerceAtLeast(0L).milliseconds
                } catch (_: DateTimeParseException) {
                    null
                }
        }?.coerceIn(Duration.ZERO, policy.retryMaxDelay)
            ?: exponentialDelay(attempt)

    fun ioDelay(attempt: Int): Duration = exponentialDelay(attempt)

    private fun exponentialDelay(attempt: Int): Duration =
        policy.retryBaseDelay * (attempt + 1) + jitterMillis(attempt).milliseconds
}
