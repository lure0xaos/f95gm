package f95gm.server.state.runtime

import java.util.concurrent.atomic.AtomicBoolean

/** Gives independently-owned resources one consistent, idempotent close boundary. */
internal class CloseGuard {
    private val closed = AtomicBoolean(false)

    val isClosed: Boolean get() = closed.get()

    fun closeOnce(block: () -> Unit) {
        if (closed.compareAndSet(false, true)) block()
    }

    suspend fun closeOnceSuspending(block: suspend () -> Unit) {
        if (closed.compareAndSet(false, true)) block()
    }
}
