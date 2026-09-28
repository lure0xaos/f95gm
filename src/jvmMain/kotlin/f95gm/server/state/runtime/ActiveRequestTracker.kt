package f95gm.server.state.runtime

import f95gm.server.kernel.CloseGuard

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

internal class ActiveRequestTracker {
    private val mutex = Mutex()
    private var active = 0
    private var zero = CompletableDeferred<Unit>().apply { complete(Unit) }

    suspend fun enter(): RequestLease = mutex.withLock {
        if (active++ == 0) zero = CompletableDeferred()
        RequestLease { leave() }
    }

    suspend fun awaitZero(timeoutMillis: Long): Boolean =
        withTimeoutOrNull(timeoutMillis) { zero.await(); true } ?: false

    suspend fun awaitZero(timeout: Duration): Boolean =
        withTimeoutOrNull(timeout) { zero.await(); true } ?: false

    suspend fun activeCount(): Int = mutex.withLock { active }

    private suspend fun leave() = mutex.withLock {
        if (--active == 0) zero.complete(Unit)
    }

    class RequestLease internal constructor(private val release: suspend () -> Unit) {
        private val closeGuard = CloseGuard()

        suspend fun close() = closeGuard.closeOnceSuspending(release)
    }
}
