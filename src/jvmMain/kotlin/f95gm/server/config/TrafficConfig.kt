package f95gm.server.config

internal data class TrafficConfig(
    val ipRequestsPerMinute: RequestsPerMinute = RequestsPerMinute(RateLimitDefaults.API_REQUESTS_PER_MINUTE),
    val accountRequestsPerMinute: RequestsPerMinute = RequestsPerMinute(RateLimitDefaults.API_REQUESTS_PER_MINUTE / 2),
    val rateLimitWindowMillis: Millis = Millis(RateLimitDefaults.WINDOW_MILLIS),
    val maxRateLimitBuckets: RateLimitBucketCount = RateLimitBucketCount(RateLimitDefaults.MAX_BUCKETS),
    val circuitBreakerOpenMillis: Millis = Millis(ServerDefaults.CIRCUIT_BREAKER_OPEN_MILLIS),
    val maxCircuitBreakerEntries: CircuitBreakerEntryCount = CircuitBreakerEntryCount(ServerDefaults.MAX_CIRCUIT_BREAKER_ENTRIES)
) {
    companion object {
        fun from(properties: ConfigProperties) = TrafficConfig(
            ipRequestsPerMinute = RequestsPerMinute(properties.positiveInt(Keys.API_REQUESTS_PER_MINUTE)),
            accountRequestsPerMinute = RequestsPerMinute(properties.positiveInt(Keys.ACCOUNT_REQUESTS_PER_MINUTE)),
            rateLimitWindowMillis = properties.positiveDuration(Keys.RATE_LIMIT_WINDOW),
            maxRateLimitBuckets = RateLimitBucketCount(properties.positiveInt(Keys.MAX_RATE_LIMIT_BUCKETS)),
            circuitBreakerOpenMillis = properties.positiveDuration(Keys.CIRCUIT_BREAKER_OPEN),
            maxCircuitBreakerEntries = CircuitBreakerEntryCount(properties.positiveInt(Keys.MAX_CIRCUIT_BREAKER_ENTRIES))
        )
    }

    fun validationIssues(): List<ConfigurationIssue> = configurationIssues {
        positive(ipRequestsPerMinute.value.toLong(), "IP request limit must be positive")
        positive(accountRequestsPerMinute.value.toLong(), "Account request limit must be positive")
        positive(rateLimitWindowMillis.value, "Rate-limit window must be positive")
        positive(maxRateLimitBuckets.value.toLong(), "Maximum rate-limit buckets must be positive")
        positive(circuitBreakerOpenMillis.value, "Circuit-breaker open time must be positive")
        positive(maxCircuitBreakerEntries.value.toLong(), "Maximum circuit-breaker entries must be positive")
        atMost(accountRequestsPerMinute.value.toLong(), ipRequestsPerMinute.value.toLong(), "Account request limit must not exceed IP request limit")
    }

    fun validate() = validationIssues().throwValidation("Traffic configuration")
}
