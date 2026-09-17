package f95gm.server.cache

import kotlinx.coroutines.CancellationException

/** Keeps cancellation from being converted into an ordinary operational failure. */
internal fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}
