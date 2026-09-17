package f95gm.server.persistence.database

import f95gm.server.kernel.RetryDecision
import f95gm.server.kernel.RetryAttemptLimit
import f95gm.server.kernel.retryBlocking
import f95gm.server.kernel.retrySuspending
import f95gm.domain.api.RetryJitter
import kotlin.time.Duration

/** Shared retry policy execution for blocking JDBC and suspend-aware callers. */
internal class DatabaseRetryExecutor(
    private val policy: DatabaseRetryPolicy,
    private val jitter: RetryJitter = RetryJitter { _, _ -> Duration.ZERO }
) {
    fun <T> blocking(sleep: (Duration) -> Unit, operation: () -> T): T = retryBlocking(
        maxAttempts = RetryAttemptLimit(policy.maxRetries + 1),
        delay = { attempt -> policy.delayWithJitter(attempt, jitter) },
        sleep = sleep,
        operation = { operation() },
        isRetryable = policy::isRetryable,
        maxTotalDelay = policy.maxTotalRetryDelay
    )

    suspend fun <T> suspending(operation: suspend (attempt: Int) -> RetryDecision<T>): T =
        retrySuspending(
            maxAttempts = RetryAttemptLimit(policy.maxRetries + 1),
            maxTotalDelay = policy.maxTotalRetryDelay,
            operation = operation
        )
}
