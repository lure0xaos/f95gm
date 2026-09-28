package f95gm.server.config

/** Subsystem-oriented validation entry points used by the aggregate config validator. */
internal object ServerConfigValidators {
    fun timeouts(value: TimeoutConfig) = value.validationIssues()
    fun limits(value: ResourceLimits, timeouts: TimeoutConfig) = value.validationIssues(timeouts)
    fun traffic(value: TrafficConfig) = value.validationIssues()
    fun upstreamRetry(value: UpstreamRetryConfig) = value.validationIssues()
    fun databaseRetry(value: DatabaseRetryConfig) = value.validationIssues()

    fun crossField(config: ServerConfig): List<ConfigurationIssue> = buildList {
        if (config.traffic.rateLimitWindowMillis.value < 1_000L) {
            add(ConfigurationIssue("Rate-limit window must be at least one second"))
        }
    }

    fun deploymentMode(value: DeploymentMode): List<ConfigurationIssue> =
        if (value == DeploymentMode.SINGLE_INSTANCE) emptyList()
        else listOf(ConfigurationIssue("Unsupported deployment mode: $value", ConfigurationValidationCode.UNSUPPORTED_VALUE))
}
