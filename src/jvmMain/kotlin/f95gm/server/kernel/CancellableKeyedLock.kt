package f95gm.server.kernel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

/** Bounded keyed mutual exclusion with cancellation-aware waiters and lifecycle closure. */
internal class CancellableKeyedLock<K>(maxEntries: Int = 4_096) : LifecycleResource {
    private val capacity = EntryCapacity(maxEntries)

    internal class Entry {
        val permit = Channel<Unit>(capacity = 1).also { it.trySend(Unit) }
        val cancelled = CompletableDeferred<Unit>()
        val users = AtomicInteger()
        val activeUsers = AtomicInteger()
    }

    internal class Lease<K> internal constructor(
        internal val key: K,
        internal val entry: Entry
    ) {
        val released = AtomicBoolean(false)
    }

    internal data class CancellationCounts(val waiters: Int, val active: Int)

    private val lock = ResourceLock()
    private val entries = AccessOrderedStore<K, Entry>()
    private val closeGuard = CloseGuard()

    val size: Int get() = lock.withLock { entries.size }

    fun acquire(key: K): Lease<K> = lock.withLock {
        check(!closeGuard.isClosed) { "Keyed lock is closed" }
        val entry = entries[key] ?: Entry().also { entries.put(key, it) }
        entry.users.incrementAndGet()
        trimIdleEntries()
        Lease(key, entry)
    }

    suspend fun <T> withLock(lease: Lease<K>, block: suspend () -> T): T = select {
        if (lease.entry.cancelled.isCompleted) throw CancellationException("Keyed lock was cancelled")
        lease.entry.cancelled.onAwait {
            throw CancellationException("Keyed lock was cancelled")
        }
        lease.entry.permit.onReceive {
            if (lease.entry.cancelled.isCompleted) {
                lease.entry.permit.trySend(Unit)
                throw CancellationException("Keyed lock was cancelled")
            }
            lease.entry.activeUsers.incrementAndGet()
            try {
                block()
            } finally {
                lease.entry.activeUsers.decrementAndGet()
                lease.entry.permit.trySend(Unit)
            }
        }
    }

    fun release(lease: Lease<K>) {
        if (!lease.released.compareAndSet(false, true)) return
        lock.withLock {
            if (lease.entry.users.decrementAndGet() == 0) {
                if (entries[lease.key] === lease.entry) trimIdleEntries()
            }
        }
    }

    /** Cancels current waiters and holders while keeping this lock reusable. */
    fun cancelWaiters(): CancellationCounts = lock.withLock {
        cancelEntriesLocked()
    }

    private fun cancelEntriesLocked(): CancellationCounts {
        var waiters = 0
        var active = 0
        entries.snapshot().forEach { snapshot ->
            val entryActive = snapshot.value.activeUsers.get()
            active += entryActive
            waiters += (snapshot.value.users.get() - entryActive).coerceAtLeast(0)
            snapshot.value.cancelled.complete(Unit)
            snapshot.value.permit.close()
        }
        entries.clear()
        return CancellationCounts(waiters, active)
    }

    fun clear() = cancelWaiters()

    override val isClosed: Boolean get() = closeGuard.isClosed
    override fun close() = closeGuard.closeOnce { lock.withLock { cancelEntriesLocked() } }

    private fun trimIdleEntries() {
        while (entries.size > capacity.value) {
            val idle = entries.snapshot().firstOrNull { it.value.users.get() == 0 } ?: return
            entries.remove(idle.key)
        }
    }
}
