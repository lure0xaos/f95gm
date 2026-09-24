package f95gm.server.state.runtime

import f95gm.server.config.Millis
import kotlinx.coroutines.delay

internal sealed interface RetryDecision<out T> {
    data class Return<T>(val value: T) : RetryDecision<T>
    data class Again(val delayMillis: Millis) : RetryDecision<Nothing>
}

internal inline fun <T> retryBlocking(
    maxAttempts: Int,
    delayMillis: (Int) -> Long,
    sleep: (Long) -> Unit,
    operation: (Int) -> T,
    isRetryable: (Throwable) -> Boolean
): T {
    require(maxAttempts > 0)
    for (attempt in 0 until maxAttempts) {
        try {
            return operation(attempt)
        } catch (error: Exception) {
            if (!isRetryable(error) || attempt == maxAttempts - 1) throw error
            sleep(delayMillis(attempt + 1))
        }
    }
    error("Retry operation completed without a result")
}

internal suspend fun <T> retrySuspending(
    maxAttempts: Int,
    pause: suspend (Long) -> Unit = { delay(it) },
    operation: suspend (Int) -> RetryDecision<T>
): T {
    require(maxAttempts > 0)
    repeat(maxAttempts) { attempt ->
        when (val decision = operation(attempt)) {
            is RetryDecision.Return -> return decision.value
            is RetryDecision.Again -> pause(decision.delayMillis.value.coerceAtLeast(0L))
        }
    }
    error("Retry operation completed without a result")
}
