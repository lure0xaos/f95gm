package f95gm.client.data.api

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.ZERO
import kotlin.random.Random
import f95gm.domain.api.parseRetryAfterSeconds
import f95gm.domain.api.RetryDelayPolicy
import f95gm.domain.api.RetryJitter

internal data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = 150.milliseconds,
    val maxDelay: Duration = 2.seconds,
    val jitter: RetryJitter = RetryJitter { _, requested ->
        if (requested == ZERO) requested else {
            val multiplier = 1.0 + Random.nextDouble(-0.2, 0.2)
            (requested * multiplier).coerceAtLeast(ZERO)
        }
    },
    val retryableStatuses: Set<Int> = setOf(429, 502, 503, 504)
) {
    private val delayPolicy = RetryDelayPolicy(initialDelay, maxDelay) { index, _ ->
        val exponential = (initialDelay * (1 shl index.coerceAtMost(30))).coerceAtMost(maxDelay)
        jitter.delay(index, exponential) - exponential
    }

    init {
        require(maxAttempts >= 1)
        require(!initialDelay.isNegative())
        require(maxDelay >= initialDelay)
        require(retryableStatuses.all { it in 100..599 })
    }

    fun retryDelay(attempt: Int, serverHint: Duration? = null): Duration {
        return delayPolicy.delay((attempt - 1).coerceAtLeast(0), serverHint)
    }

    fun shouldRetryStatus(status: Int): Boolean = status in retryableStatuses
}

internal object ApiRetryability {
    fun isRetryable(error: Throwable): Boolean = when (error) {
        is ApiTransportException -> true
        is ApiException -> error.retryable
        else -> false
    }
}

internal suspend fun <T> withTransportRetry(
    policy: RetryPolicy,
    pause: suspend (Duration) -> Unit = { kotlinx.coroutines.delay(it) },
    retryableResult: (T) -> Duration? = { null },
    block: suspend () -> T
): T {
    var attempt = 1
    while (true) {
        try {
            val result = block()
            val serverHint = retryableResult(result)
            if (serverHint == null || attempt >= policy.maxAttempts) return result
            pause(policy.retryDelay(attempt, serverHint))
            attempt++
        } catch (error: ApiTransportException) {
            if (!ApiRetryability.isRetryable(error) || attempt >= policy.maxAttempts) throw error
            pause(policy.retryDelay(attempt))
            attempt++
        }
    }
}
