package f95gm.domain.api

import kotlin.time.Duration.Companion.seconds

enum class RetryAfterSource { DELTA_SECONDS, HTTP_DATE }

data class RetryAfterHint(val seconds: Long, val source: RetryAfterSource) {
    val duration = seconds.seconds
}

/** Parses the portable delta-seconds form of HTTP Retry-After. */
fun parseRetryAfterSeconds(value: String?): RetryAfterHint? = value
    ?.trim()
    ?.toLongOrNull()
    ?.takeIf { it >= 0L }
    ?.let { RetryAfterHint(it, RetryAfterSource.DELTA_SECONDS) }

/** Platform parser for both HTTP Retry-After wire forms with an injected current time. */
internal expect fun parseRetryAfter(value: String?, nowEpochMillis: Long): RetryAfterHint?
