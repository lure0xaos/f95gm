package f95gm.server.cache

import f95gm.server.kernel.CancellableKeyedLock
import f95gm.server.kernel.CompletionSignal
import f95gm.server.kernel.ResourceLock
/** Coordinates suspendable cache loads without sharing state with blocking loads. */
internal class AsyncCacheLoadCoordinator<K>(
    private val onShutdownRejection: () -> Unit = {},
    private val onWaiterCancellation: (Int) -> Unit = {},
    private val onActiveLoadInvalidation: (Int) -> Unit = {}
) : CacheLoadLifecycle {
    private val locks = CancellableKeyedLock<K>()
    private val lifecycleLock = ResourceLock()
    private val shutdown = CompletionSignal()
    private var closed = false
    private var activeLoads = 0

    override val isClosed: Boolean get() = lifecycleLock.withLock { closed }

    fun acquire(key: K): CancellableKeyedLock.Lease<K> = lifecycleLock.withLock {
        if (closed) {
            onShutdownRejection()
            error("Async cache load coordinator is closed")
        }
        locks.acquire(key)
    }

    suspend fun <T> withLock(load: CancellableKeyedLock.Lease<K>, block: suspend () -> T): T = locks.withLock(load) {
        lifecycleLock.withLock { activeLoads++ }
        try {
            block()
        } finally {
            lifecycleLock.withLock {
                activeLoads--
                completeShutdownIfIdleLocked()
            }
        }
    }

    fun release(load: CancellableKeyedLock.Lease<K>) = locks.release(load)

    /** Cancels queued waiters; an already-running loader is allowed to finish. */
    override fun clear() {
        val counts = locks.cancelWaiters()
        onWaiterCancellation(counts.waiters)
        onActiveLoadInvalidation(counts.active)
    }

    override fun close() {
        lifecycleLock.withLock {
            if (closed) return
            closed = true
            val counts = locks.cancelWaiters()
            onWaiterCancellation(counts.waiters)
            onActiveLoadInvalidation(counts.active)
            locks.close()
            completeShutdownIfIdleLocked()
        }
    }

    override suspend fun closeAndJoin() {
        close()
        shutdown.await()
    }

    private fun completeShutdownIfIdleLocked() {
        if (closed && activeLoads == 0) shutdown.complete()
    }
}
