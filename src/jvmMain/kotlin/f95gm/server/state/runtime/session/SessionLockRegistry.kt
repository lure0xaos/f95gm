package f95gm.server.state.runtime.session

import f95gm.domain.values.F95SessionId
import f95gm.server.state.runtime.CloseGuard
import f95gm.server.state.runtime.LifecycleResource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal class SessionLockRegistry : LifecycleResource {
    private data class Entry(val mutex: Mutex, val users: AtomicInteger = AtomicInteger())

    private val entries = ConcurrentHashMap<F95SessionId, Entry>()
    private val closeGuard = CloseGuard()
    override val isClosed: Boolean get() = closeGuard.isClosed

    suspend fun <T> withLock(sessionId: F95SessionId, block: suspend () -> T): T {
        check(!closeGuard.isClosed) { "Session lock registry is closed" }
        val entry = entries.compute(sessionId) { _, current ->
            check(!closeGuard.isClosed) { "Session lock registry is closed" }
            (current ?: Entry(Mutex())).also { it.users.incrementAndGet() }
        } ?: error("Unable to create session lock")
        return try {
            entry.mutex.withLock { block() }
        } finally {
            if (entry.users.decrementAndGet() == 0) entries.remove(sessionId, entry)
        }
    }

    override fun close() {
        closeGuard.closeOnce { entries.entries.removeIf { it.value.users.get() == 0 } }
    }
}
