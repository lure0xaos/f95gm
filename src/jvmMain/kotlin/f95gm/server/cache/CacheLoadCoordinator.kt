package f95gm.server.cache

import f95gm.server.kernel.CompletionCell
import f95gm.server.kernel.CompletionSignal
import f95gm.server.kernel.ResourceLock
import java.util.concurrent.CompletionException
import java.util.HashMap
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Coordinates blocking cache loads and their terminal lifecycle. */
internal class CacheLoadCoordinator<K, V>(
    private val onWaiterCancellation: (Int) -> Unit = {},
    private val onActiveLoadInvalidation: () -> Unit = {},
    private val onStaleCompletion: () -> Unit = {}
) : CacheLoadLifecycle {
    private val lock = ResourceLock()
    private val loads = HashMap<K, Load<V>>()
    private val shutdown = CompletionSignal()
    private var closed = false
    private var activeOwners = 0

    override val isClosed: Boolean get() = lock.withLock { closed }

    fun claim(key: K): Load<V> = lock.withLock {
        check(!closed) { "Cache load coordinator is closed" }
        loads[key]?.asWaiter() ?: Load<V>(owner = true).also {
            loads[key] = it
            activeOwners++
        }
    }

    fun complete(key: K, load: Load<V>, value: V) {
        finish(key, load) { load.complete(value) }
    }

    fun fail(key: K, load: Load<V>, error: Throwable) {
        finish(key, load) { load.fail(error) }
    }

    /** Marks the synchronous owner method as returned, not merely published. */
    fun ownerFinished(load: Load<V>) {
        if (!load.owner) return
        lock.withLock {
            check(activeOwners > 0) { "Cache load owner completed without an active owner" }
            activeOwners--
            completeShutdownIfIdleLocked()
        }
    }

    override fun clear() {
        val snapshot = lock.withLock { loads.entries.toList() }
        snapshot.forEach { (key, load) ->
            finish(key, load) {
                val cancelled = load.invalidate(CancellationException("Cache loads cleared"))
                if (cancelled) {
                    onWaiterCancellation(load.waiterCount)
                    onActiveLoadInvalidation()
                }
                cancelled
            }
        }
    }

    override fun close() {
        lock.withLock {
            if (closed) return
            closed = true
        }
        clear()
        lock.withLock { completeShutdownIfIdleLocked() }
    }

    override suspend fun closeAndJoin() {
        close()
        shutdown.await()
    }

    fun await(load: Load<V>): V = try {
        load.await()
    } catch (error: InterruptedException) {
        Thread.currentThread().interrupt()
        throw CompletionException(error)
    }

    private fun finish(key: K, load: Load<V>, completion: () -> Boolean) {
        if (!completion()) {
            if (load.wasInvalidated) onStaleCompletion()
            return
        }
        lock.withLock {
            if (loads[key] === load) loads.remove(key)
            completeShutdownIfIdleLocked()
        }
    }

    private fun completeShutdownIfIdleLocked() {
        if (closed && activeOwners == 0) shutdown.complete()
    }

    class Load<V> private constructor(private val state: State<V>, val owner: Boolean) {
        private class State<V> {
            val completion = CompletionCell<V>()
            val waiterCount = AtomicInteger()
            val invalidated = AtomicBoolean()
        }

        internal constructor(owner: Boolean) : this(State(), owner)

        fun asWaiter(): Load<V> = Load(state, false).also { state.waiterCount.incrementAndGet() }

        val waiterCount: Int get() = state.waiterCount.get()
        val wasInvalidated: Boolean get() = state.invalidated.get()

        fun complete(value: V): Boolean = synchronized(state) {
            state.completion.complete(value)
        }

        fun fail(error: Throwable): Boolean = synchronized(state) {
            state.completion.fail(error)
        }

        fun invalidate(error: Throwable): Boolean = synchronized(state) {
            if (!state.completion.fail(error)) return false
            state.invalidated.set(true)
            true
        }

        @Throws(InterruptedException::class)
        fun await(): V = when (val result = state.completion.await()) {
            is CompletionCell.Outcome.Success -> result.value
            is CompletionCell.Outcome.Failure -> throw when (val error = result.error) {
                is RuntimeException, is Error -> error
                else -> CompletionException(error)
            }
        }
    }
}
