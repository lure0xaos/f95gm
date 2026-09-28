package f95gm.server.config.protocol

/** HTTP header names emitted by the request rate-limit policy. */
internal object RateLimitProtocolConfig {
    const val LIMIT = "X-RateLimit-Limit"
    const val REMAINING = "X-RateLimit-Remaining"
    const val RESET = "X-RateLimit-Reset"
    const val IP_LIMIT = "X-RateLimit-IP-Limit"
    const val IP_REMAINING = "X-RateLimit-IP-Remaining"
    const val IP_RESET = "X-RateLimit-IP-Reset"
    const val ACCOUNT_LIMIT = "X-RateLimit-Account-Limit"
    const val ACCOUNT_REMAINING = "X-RateLimit-Account-Remaining"
    const val ACCOUNT_RESET = "X-RateLimit-Account-Reset"
}
