package f95gm.server.state.runtime

import f95gm.server.kernel.LifecycleResource
internal inline fun closeSafely(resource: LifecycleResource, crossinline onFailure: (Exception) -> Unit) {
    runCleanupBoundary(onFailure) { resource.close() }
}

@Suppress("TooGenericExceptionCaught") // Awaitable resource closure shares the lifecycle aggregation boundary.
internal suspend inline fun closeAwaitablySafely(crossinline close: suspend () -> Unit, onFailure: (Exception) -> Unit) {
    try {
        close()
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        onFailure(failure)
    }
}
