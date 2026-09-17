package f95gm.domain.api

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal actual fun parseRetryAfter(value: String?, nowEpochMillis: Long): RetryAfterHint? = value?.let { raw ->
    parseRetryAfterSeconds(raw) ?: parseRetryAfterDate(raw, nowEpochMillis)
}

private fun parseRetryAfterDate(value: String, nowEpochMillis: Long): RetryAfterHint? =
    try {
        val target = ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
        RetryAfterHint(
            seconds = ((target.toEpochMilli() - nowEpochMillis).coerceAtLeast(0L) + 999L) / 1_000L,
            source = RetryAfterSource.HTTP_DATE
        )
    } catch (_: DateTimeParseException) {
        null
    }
