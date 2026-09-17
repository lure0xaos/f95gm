package f95gm.server.kernel

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CancellationException
import java.util.HashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class SingleFlight<K, V>(
    private val scope: CoroutineScope,
    private val shutdownTimeout: Duration = 10.seconds,
    private val onFailure: (String, Exception) -> Unit = { _, _ -> }
) : AwaitableLifecycleResource {
    init { require(shutdownTimeout.isPositive()) }
    private val flights = HashMap<K, CompletableDeferred<V>>()
    private val producerJobs = mutableSetOf<Job>()
    private val stateLock = ResourceLock()
    private val shutdown = CompletionSignal()
    private val lifecycle = LifecycleGate()
    override val isClosed: Boolean get() = lifecycle.isClosed

    @Suppress("TooGenericExceptionCaught", "ThrowsCount") // The producer boundary must complete the shared deferred for ordinary and fatal failures.
    suspend fun run(key: K, block: suspend () -> V): V {
        val created = CompletableDeferred<V>()
        val flight = stateLock.withLock {
            check(lifecycle.isOpen) { "Single-flight registry is closed" }
            flights.putIfAbsent(key, created) ?: created
        }
        if (flight !== created) return flight.await()
        val producer = scope.launch(start = CoroutineStart.LAZY) {
            try {
                created.complete(block())
            } catch (cancelled: CancellationException) {
                created.completeExceptionally(cancelled)
                throw cancelled
            } catch (failure: Exception) {
                onFailure("single-flight", failure)
                created.completeExceptionally(failure)
            } catch (fatal: Error) {
                created.completeExceptionally(fatal)
                throw fatal
            } finally {
                stateLock.withLock {
                    if (flights[key] === created) flights.remove(key)
                }
            }
        }
        stateLock.withLock {
            if (!lifecycle.isOpen) {
                flights.remove(key, created)
                created.completeExceptionally(CancellationException("Single-flight registry is closed"))
                producer.cancel()
                completeShutdownIfIdleLocked()
            } else {
                producerJobs += producer
                producer.invokeOnCompletion {
                    stateLock.withLock {
                        producerJobs.remove(producer)
                        completeShutdownIfIdleLocked()
                    }
                }
                producer.start()
            }
        }
        return created.await()
    }

    override suspend fun closeAndJoin() {
        close()
        try {
            withTimeout(remainingShutdownTimeout(shutdownTimeout)) { shutdown.await() }
        } catch (error: kotlinx.coroutines.TimeoutCancellationException) {
            val violation = shutdownTimeoutViolation(
                "single-flight producers",
                error,
                activeWork = stateLock.withLock { producerJobs.size }
            )
            shutdown.fail(violation)
            throw violation
        }
    }

    override fun close() {
        stateLock.withLock {
            if (!lifecycle.beginClose()) return
            flights.values.forEach { it.completeExceptionally(CancellationException("Single-flight registry is closed")) }
            flights.clear()
            producerJobs.forEach(Job::cancel)
            completeShutdownIfIdleLocked()
        }
    }

    private fun completeShutdownIfIdleLocked() {
        if (lifecycle.isClosing && producerJobs.isEmpty()) {
            lifecycle.complete()
            shutdown.complete()
        }
    }
}
