package f95gm.server.kernel

import kotlinx.coroutines.CancellationException

/** Shared synchronous observer boundary; cancellation is never converted to an observer failure. */
@Suppress("TooGenericExceptionCaught")
internal inline fun runObserverFailureBoundary(
    crossinline onFailure: (Exception) -> Unit,
    block: () -> Unit
) {
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        onFailure(failure)
    }
}
