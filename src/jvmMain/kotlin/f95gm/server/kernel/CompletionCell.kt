package f95gm.server.kernel

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/** Shared one-shot outcome state for both blocking and suspending completion bridges. */
internal class CompletionState<T> {
    private val completed = java.util.concurrent.CountDownLatch(1)
    private val deferred = CompletableDeferred<CompletionCell.Outcome<T>>()
    private val outcome = AtomicReference<CompletionCell.Outcome<T>?>(null)

    fun complete(value: T): Boolean = completeWith(CompletionCell.Outcome.Success(value))
    fun fail(error: Throwable): Boolean = completeWith(CompletionCell.Outcome.Failure(error))

    @Throws(InterruptedException::class)
    fun awaitBlocking(): CompletionCell.Outcome<T> {
        completed.await()
        return checkNotNull(outcome.get()) { "Completion state completed without an outcome" }
    }

    suspend fun await(): CompletionCell.Outcome<T> = deferred.await()

    @Throws(InterruptedException::class)
    fun awaitBlocking(timeout: Duration): CompletionCell.Outcome<T>? {
        if (!completed.await(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)) return null
        return checkNotNull(outcome.get()) { "Completion state completed without an outcome" }
    }

    private fun completeWith(result: CompletionCell.Outcome<T>): Boolean =
        outcome.compareAndSet(null, result).also {
            if (it) {
                completed.countDown()
                deferred.complete(result)
            }
        }
}

/** A one-shot completion cell for blocking JVM bridges. */
internal class CompletionCell<T> {
    private val state = CompletionState<T>()

    fun complete(value: T): Boolean = state.complete(value)

    fun fail(error: Throwable): Boolean = state.fail(error)

    @Throws(InterruptedException::class)
    fun await(): Outcome<T> = state.awaitBlocking()

    @Throws(InterruptedException::class)
    fun await(timeout: Duration): Outcome<T>? = state.awaitBlocking(timeout)

    sealed interface Outcome<out T> {
        data class Success<T>(val value: T) : Outcome<T>
        data class Failure(val error: Throwable) : Outcome<Nothing>
    }
}
