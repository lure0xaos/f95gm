package f95gm.server.http.request.ratelimit

import f95gm.server.config.Millis

/** Separate budgets make the enforcement policy explicit and independently tunable. */
internal data class RateLimitPolicy(
    val ipRequestsPerWindow: RequestBudget,
    val accountRequestsPerWindow: RequestBudget,
    val windowMillis: Millis
) {
    init {
        require(windowMillis.value > 0) { "Rate-limit window must be positive" }
    }

    companion object {
        fun fromApiRequestsPerMinute(limit: Int, windowMillis: Long): RateLimitPolicy =
            RateLimitPolicy(RequestBudget(limit), RequestBudget((limit / 2).coerceAtLeast(1)), Millis(windowMillis))

        fun configured(ipLimit: Int, accountLimit: Int, windowMillis: Long): RateLimitPolicy =
            RateLimitPolicy(RequestBudget(ipLimit), RequestBudget(accountLimit), Millis(windowMillis))
    }
}
