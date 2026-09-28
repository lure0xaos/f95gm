package f95gm.server.kernel

import f95gm.domain.api.RetryBudget
import kotlinx.coroutines.delay
import kotlin.time.Duration

internal enum class RetryFailureReason {
    DELAY_BUDGET_EXHAUSTED,
    NO_RESULT
}

internal class RetryExecutor(
    val reason: RetryFailureReason,
    val requestedDelay: Duration? = null,
    val remainingDelay: Duration? = null
) : IllegalStateException(reason.name)

@JvmInline
internal value class RetryAttemptLimit(val value: Int) {
    init { require(value > 0) { "Retry attempt limit must be positive" } }
}

private sealed interface BlockingAttempt<out T> {
    data class Succeeded<T>(val value: T) : BlockingAttempt<T>
    data class Failed(val error: Throwable) : BlockingAttempt<Nothing>
}

private fun retryBudget(maxTotalDelay: Duration?): RetryBudget? {
    maxTotalDelay?.let { require(!it.isNegative()) }
    return maxTotalDelay?.let(::RetryBudget)
}

private fun RetryBudget.reserveOrThrow(requested: Duration): Duration = reserve(requested)
    ?: throw RetryExecutor(RetryFailureReason.DELAY_BUDGET_EXHAUSTED, requested, remaining)

internal fun <T> retryBlocking(
    maxAttempts: RetryAttemptLimit,
    delay: (Int) -> Duration,
    sleep: (Duration) -> Unit,
    operation: (Int) -> T,
    isRetryable: (Throwable) -> Boolean,
    maxTotalDelay: Duration? = null
): T {
    val budget = retryBudget(maxTotalDelay)
    for (attempt in 0 until maxAttempts.value) {
        val result = captureBlocking { operation(attempt) }
        if (result is BlockingAttempt.Succeeded) return result.value
        val error = (result as BlockingAttempt.Failed).error
        if (!isRetryable(error) || attempt == maxAttempts.value - 1) return rethrow(error)
        val requestedDelay = delay(attempt + 1)
        val retryDelay = budget?.reserveOrThrow(requestedDelay) ?: requestedDelay
        sleep(retryDelay)
    }
    return retryFailure()
}

@Suppress("TooGenericExceptionCaught") // Blocking retries record ordinary failures; interruption is preserved below.
private fun <T> captureBlocking(operation: () -> T): BlockingAttempt<T> = try {
    BlockingAttempt.Succeeded(operation())
} catch (interrupted: InterruptedException) {
    Thread.currentThread().interrupt()
    BlockingAttempt.Failed(interrupted)
} catch (error: Exception) {
    BlockingAttempt.Failed(error)
}

private fun retryFailure(): Nothing = throw RetryExecutor(RetryFailureReason.NO_RESULT)
private fun rethrow(error: Throwable): Nothing = throw error

internal suspend fun <T> retrySuspending(
    maxAttempts: RetryAttemptLimit,
    pause: suspend (Duration) -> Unit = { delay(it) },
    maxTotalDelay: Duration? = null,
    operation: suspend (Int) -> RetryDecision<T>
): T {
    val budget = retryBudget(maxTotalDelay)
    repeat(maxAttempts.value) { attempt ->
        when (val decision = operation(attempt)) {
            is RetryDecision.Return -> return decision.value
            is RetryDecision.Again -> {
                if (attempt == maxAttempts.value - 1) retryFailure()
                val requested = decision.delay.coerceAtLeast(Duration.ZERO)
                val pauseFor = budget?.reserveOrThrow(requested) ?: requested
                pause(pauseFor)
            }
        }
    }
    retryFailure()
}
