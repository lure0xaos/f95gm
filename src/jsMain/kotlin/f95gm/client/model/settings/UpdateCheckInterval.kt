package f95gm.client.model.settings

internal enum class UpdateCheckInterval(val millis: Long) {
    DISABLED(0L),
    FIFTEEN_MINUTES(15 * 60 * 1000L),
    HOUR(60 * 60 * 1000L),
    SIX_HOURS(6 * 60 * 60 * 1000L),
    DAY(24 * 60 * 60 * 1000L);

    companion object {
        fun fromMillis(millis: Long?): UpdateCheckInterval =
            entries.firstOrNull { it.millis == millis } ?: FIFTEEN_MINUTES
    }
}
