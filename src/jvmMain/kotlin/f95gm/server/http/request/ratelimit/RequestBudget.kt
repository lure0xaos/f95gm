package f95gm.server.http.request.ratelimit

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import f95gm.server.kernel.EntryCapacity

@JvmInline
internal value class RequestRateLimit(val value: Int) {
    init {
        require(value > 0) { "Request rate limit must be positive" }
    }
}

/** Compatibility name retained for callers that model the value as a budget. */
internal typealias RequestBudget = RequestRateLimit

@JvmInline
internal value class RateLimitWindow(val duration: Duration) {
    init { require(duration.isPositive()) { "Rate-limit window must be positive" } }

    val value: Long get() = duration.inWholeMilliseconds

    companion object {
        fun from(duration: Duration): RateLimitWindow = RateLimitWindow(duration)
        fun fromMillis(value: Long): RateLimitWindow = from(value.milliseconds)
    }
}

internal typealias BucketCapacity = EntryCapacity
