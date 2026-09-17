package f95gm.server.state.runtime.session

import f95gm.server.kernel.ServerInvariantViolation
import f95gm.domain.values.F95SessionId
import f95gm.server.kernel.CancellableKeyedLock
import f95gm.server.kernel.LifecycleResource

internal class SessionLockRegistry(maxEntries: Int = 4_096) : LifecycleResource {
    private val locks = CancellableKeyedLock<F95SessionId>(maxEntries)
    @Volatile
    private var closed = false
    override val isClosed: Boolean get() = closed

    suspend fun <T> withLock(sessionId: F95SessionId, block: suspend () -> T): T {
        if (closed) return sessionLockClosed()
        val lease = try { locks.acquire(sessionId) } catch (_: IllegalStateException) { return sessionLockClosed() }
        return try {
            locks.withLock(lease, block)
        } finally {
            locks.release(lease)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        locks.close()
    }
}

private fun sessionLockClosed(): Nothing = throw ServerInvariantViolation("Session lock registry is closed")
