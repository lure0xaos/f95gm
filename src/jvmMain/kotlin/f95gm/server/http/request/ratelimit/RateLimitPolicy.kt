package f95gm.server.http.request.ratelimit

/** Separate budgets make the enforcement policy explicit and independently tunable. */
internal data class RateLimitPolicy(
    val ipRequestsPerWindow: RequestRateLimit,
    val accountRequestsPerWindow: RequestRateLimit,
    val windowMillis: RateLimitWindow
) {
    companion object {
        fun fromApiRequestsPerMinute(limit: Int, windowMillis: Long): RateLimitPolicy =
            RateLimitPolicy(RequestRateLimit(limit), RequestRateLimit((limit / 2).coerceAtLeast(1)), RateLimitWindow.fromMillis(windowMillis))

        fun configured(ipLimit: Int, accountLimit: Int, windowMillis: Long): RateLimitPolicy =
            RateLimitPolicy(RequestRateLimit(ipLimit), RequestRateLimit(accountLimit), RateLimitWindow.fromMillis(windowMillis))
    }
}
