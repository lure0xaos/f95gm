package f95gm.server.config

/** Pure comparison of two validated configuration snapshots. */
internal object ConfigurationChangeDetector {
    fun fields(previous: ServerConfig, next: ServerConfig): Set<ConfigurationChangeField> =
        restartFields(previous, next) + dynamicFields(previous, next)

    private fun restartFields(previous: ServerConfig, next: ServerConfig): Set<ConfigurationChangeField> = buildSet {
        if (previous.host != next.host || previous.port != next.port) add(ConfigurationChangeField.LISTENER)
        if (previous.database != next.database) add(ConfigurationChangeField.DATABASE)
        if (previous.staticDirectory != next.staticDirectory) add(ConfigurationChangeField.STATIC_DIRECTORY)
        if (previous.deploymentMode != next.deploymentMode) add(ConfigurationChangeField.DEPLOYMENT_MODE)
        if (previous.upstreamBaseUrl != next.upstreamBaseUrl) add(ConfigurationChangeField.UPSTREAM_ENDPOINT)
        if (previous.timeouts != next.timeouts) add(ConfigurationChangeField.HTTP_CLIENT_TIMEOUTS)
        if (previous.upstreamRetry != next.upstreamRetry) add(ConfigurationChangeField.UPSTREAM_RETRY_POLICY)
        if (previous.databaseRetry != next.databaseRetry) add(ConfigurationChangeField.DATABASE_RETRY_POLICY)
        if (previous.traffic.circuitBreakerOpenMillis != next.traffic.circuitBreakerOpenMillis ||
            previous.traffic.maxCircuitBreakerEntries != next.traffic.maxCircuitBreakerEntries
        ) add(ConfigurationChangeField.CIRCUIT_BREAKER_POLICY)
        if (previous.traffic.maxRateLimitBuckets != next.traffic.maxRateLimitBuckets) {
            add(ConfigurationChangeField.RATE_LIMIT_BUCKET_CAPACITY)
        }
    }

    private fun dynamicFields(previous: ServerConfig, next: ServerConfig): Set<ConfigurationChangeField> = buildSet {
        if (previous.limits != next.limits) add(ConfigurationChangeField.RESOURCE_LIMITS)
        if (previous.traffic.ipRequestsPerMinute != next.traffic.ipRequestsPerMinute) {
            add(ConfigurationChangeField.IP_RATE_LIMIT)
        }
        if (previous.traffic.accountRequestsPerMinute != next.traffic.accountRequestsPerMinute) {
            add(ConfigurationChangeField.ACCOUNT_RATE_LIMIT)
        }
        if (previous.traffic.rateLimitWindowMillis != next.traffic.rateLimitWindowMillis) {
            add(ConfigurationChangeField.RATE_LIMIT_WINDOW)
        }
        if (previous.openBrowser != next.openBrowser) add(ConfigurationChangeField.OPEN_BROWSER)
        if (previous.configurationDiagnostics != next.configurationDiagnostics) add(ConfigurationChangeField.DIAGNOSTICS)
    }
}
