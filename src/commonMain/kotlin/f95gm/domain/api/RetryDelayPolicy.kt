package f95gm.domain.api

import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

fun interface RetryJitter {
    fun delay(attempt: Int, baseDelay: Duration): Duration
}

/** Tracks a bounded cumulative delay budget shared by retry implementations. */
class RetryBudget(private val maximum: Duration) {
    init { require(!maximum.isNegative()) }

    private var consumed = ZERO
    val remaining: Duration get() = (maximum - consumed).coerceAtLeast(ZERO)

    fun reserve(requested: Duration): Duration? {
        val bounded = requested.coerceAtLeast(ZERO)
        if (bounded > remaining) return null
        consumed += bounded
        return bounded
    }
}

/** Shared bounded backoff calculation used by client and server retry policies. */
data class RetryDelayPolicy(
    val baseDelay: Duration,
    val maxDelay: Duration,
    val jitter: RetryJitter = RetryJitter { _, _ -> ZERO }
) {
    init {
        require(!baseDelay.isNegative())
        require(maxDelay >= baseDelay)
    }

    fun delay(attempt: Int, serverHint: Duration? = null): Duration {
        require(attempt >= 0)
        val calculated = (baseDelay * (attempt + 1) + jitter.delay(attempt, baseDelay)).coerceIn(ZERO, maxDelay)
        return (serverHint ?: calculated).coerceIn(ZERO, maxDelay)
    }
}
