package f95gm.client.model.settings

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal enum class UpdateCheckInterval(val duration: Duration) {
    DISABLED(Duration.ZERO),
    FIFTEEN_MINUTES(15.minutes),
    HOUR(1.hours),
    SIX_HOURS(6.hours),
    DAY(1.days);

    companion object {
        fun fromMillis(millis: Long?): UpdateCheckInterval =
            entries.firstOrNull { it.duration.inWholeMilliseconds == millis } ?: FIFTEEN_MINUTES
    }
}
