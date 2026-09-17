package f95gm.server.config

internal data class UpstreamRetryConfig(
    val maxAttempts: Int = 3,
    val baseDelayMillis: Millis = Millis(250L),
    val maxDelayMillis: Millis = Millis(30_000L),
    val maxTotalDelayMillis: Millis = Millis(RetryLimits.UPSTREAM_MAX_TOTAL_DELAY_MILLIS)
) {
    companion object {
        fun from(properties: ConfigProperties) = UpstreamRetryConfig(
            maxAttempts = properties.positiveInt(Keys.RETRY_MAX_ATTEMPTS),
            baseDelayMillis = properties.positiveDuration(Keys.RETRY_BASE_DELAY),
            maxDelayMillis = properties.positiveDuration(Keys.RETRY_MAX_DELAY),
            maxTotalDelayMillis = properties.positiveDuration(Keys.RETRY_MAX_TOTAL_DELAY)
        )
    }

    fun validationIssues(): List<ConfigurationIssue> = configurationIssues {
        positive(maxAttempts.toLong(), "Retry attempts must be positive")
        positive(baseDelayMillis.value, "Retry base delay must be positive")
        atLeast(maxDelayMillis.value, baseDelayMillis.value, "Retry maximum delay must not be shorter than base delay")
        positive(maxTotalDelayMillis.value, "Retry total delay must be positive")
    }

    fun validate() = validationIssues().throwValidation("Retry configuration")
}

internal data class DatabaseRetryConfig(
    val maxTotalDelayMillis: Millis = Millis(RetryLimits.DATABASE_MAX_TOTAL_DELAY_MILLIS)
) {
    fun validationIssues(): List<ConfigurationIssue> = configurationIssues {
        positive(maxTotalDelayMillis.value, "Database retry total delay must be positive")
    }
}
