package f95gm.domain.api

internal actual fun parseRetryAfter(value: String?, nowEpochMillis: Long): RetryAfterHint? =
    parseRetryAfterSeconds(value)
