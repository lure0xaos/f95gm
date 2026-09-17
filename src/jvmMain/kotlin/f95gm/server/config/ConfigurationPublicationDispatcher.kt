@file:Suppress("TooGenericExceptionCaught") // The publication worker is the terminal observer-failure boundary.

package f95gm.server.config

import f95gm.server.kernel.ResourceLock
import f95gm.server.kernel.CompletionCell
import f95gm.server.kernel.AwaitableLifecycleResource
import f95gm.server.kernel.LifecycleGate
import f95gm.server.kernel.remainingShutdownTimeout
import f95gm.server.kernel.shutdownTimeoutViolation
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Bounded, serial observer delivery without coupling publication to a future chain. */
internal class ConfigurationPublicationDispatcher(
    dispatcher: CoroutineDispatcher,
    private val maxPendingPublications: Int = DEFAULT_MAX_PENDING_PUBLICATIONS,
    private val shutdownTimeout: Duration = 10.seconds
) : AwaitableLifecycleResource {
    companion object { const val DEFAULT_MAX_PENDING_PUBLICATIONS = 1_024 }

    init {
        require(maxPendingPublications > 0)
        require(shutdownTimeout.isPositive())
    }

    private val lock = ResourceLock()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val queue = Channel<PublicationTask<*>>(maxPendingPublications)
    private val workerCompleted = CompletionCell<Unit>()
    private val executingPublication = ThreadLocal<Boolean>()
    private val activeTask = AtomicReference<PublicationTask<*>?>(null)
    private val lifecycle = LifecycleGate()
    private var closed = false
    private var pendingPublications = 0
    private var rejectedPublications = 0L
    private val worker = scope.launch {
        try {
            for (task in queue) {
                activeTask.set(task)
                executingPublication.set(true)
                try {
                    task.run()
                } finally {
                    executingPublication.remove()
                    activeTask.compareAndSet(task, null)
                }
            }
        } catch (fatal: Error) {
            requestClose()
            while (true) {
                val pending = queue.tryReceive().getOrNull() ?: break
                pending.fail(fatal)
            }
            throw fatal
        } finally {
            workerCompleted.complete(Unit)
            lifecycle.complete()
            scope.cancel()
        }
    }

    fun checkCanWait() {
        check(executingPublication.get() != true) { "Configuration observers cannot synchronously reload or close configuration" }
    }

    fun <T> enqueue(block: () -> T): PublicationTask<T> = enqueueAfterAdmission { block }

    /** Admission and snapshot commit precede observer execution; shutdown uses the same lock. */
    fun <T> enqueueAfterAdmission(prepare: () -> (() -> T)): PublicationTask<T> = lock.withLock {
        checkCanWait()
        check(!closed) { "Configuration publication dispatcher is closed" }
        if (pendingPublications >= maxPendingPublications) {
            rejectedPublications++
            throw ConfigurationPublicationRejected(maxPendingPublications)
        }
        val task = PublicationTask(prepare()) {
            lock.withLock { pendingPublications-- }
        }
        pendingPublications++
        check(queue.trySend(task).isSuccess) { "Admitted configuration publication could not be queued" }
        task
    }

    val pendingCount: Int get() = lock.withLock { pendingPublications }
    val rejectedCount: Long get() = lock.withLock { rejectedPublications }

    fun <T> await(queued: PublicationTask<T>): T {
        checkCanWait()
        return queued.await()
    }

    fun <T> dispatch(block: () -> T): T = await(enqueue(block))

    override val isClosed: Boolean get() = lifecycle.isClosed

    override fun close() {
        checkCanWait()
        requestClose()
        try {
            if (workerCompleted.await(shutdownTimeout) == null) failShutdown()
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("Interrupted while closing configuration publication dispatcher", interrupted)
        }
    }

    override suspend fun closeAndJoin() {
        checkCanWait()
        requestClose()
        try {
            withTimeout(remainingShutdownTimeout(shutdownTimeout)) {
                worker.join()
                scope.cancel()
            }
        } catch (timeout: TimeoutCancellationException) {
            failShutdown(timeout)
        }
    }

    private fun failShutdown(cause: Throwable = TimeoutException("Configuration publication shutdown deadline exceeded")): Nothing {
        val violation = shutdownTimeoutViolation("configuration publication worker", cause, activeWork = pendingCount)
        lifecycle.fail(violation)
        scope.cancel()
        activeTask.get()?.fail(violation)
        while (true) {
            val pending = queue.tryReceive().getOrNull() ?: break
            pending.fail(violation)
        }
        throw violation
    }

    private fun requestClose() {
        lock.withLock {
            if (closed) return
            closed = true
            lifecycle.beginClose()
            queue.close()
        }
    }
}

internal class ConfigurationPublicationRejected(maxPendingPublications: Int) :
    IllegalStateException("Configuration publication queue is full (limit=$maxPendingPublications)")

internal class PublicationTask<T> internal constructor(
    private val block: () -> T,
    private val onComplete: () -> Unit
) {
    private val completion = CompletionCell<T>()
    private val completionCallback = AtomicBoolean(false)

    @Suppress("TooGenericExceptionCaught") // Publication is the infrastructure boundary that records observer failures for waiters.
    internal fun run() {
        try {
            complete(block())
        } catch (failure: Exception) {
            fail(failure)
        } catch (fatal: Error) {
            fail(fatal)
            throw fatal
        }
    }

    internal fun fail(failure: Throwable) {
        completion.fail(failure)
        onCompleteIfFirstCompletion()
    }

    private fun complete(value: T) {
        completion.complete(value)
        onCompleteIfFirstCompletion()
    }

    internal fun await(): T {
        val result = try {
            completion.await()
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw java.util.concurrent.CompletionException(interrupted)
        }
        return when (result) {
            is CompletionCell.Outcome.Success -> result.value
            is CompletionCell.Outcome.Failure -> throw when (val failure = result.error) {
                is RuntimeException, is Error -> failure
                else -> IllegalStateException("Configuration publication failed", failure)
            }
        }
    }

    private fun onCompleteIfFirstCompletion() {
        if (completionCallback.compareAndSet(false, true)) onComplete()
    }
}
