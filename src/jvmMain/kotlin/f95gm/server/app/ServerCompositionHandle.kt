package f95gm.server.app

import f95gm.server.state.runtime.ServerApplicationComposition

/** Runtime lifecycle handle for the application composition owned by the composition root. */
internal class ServerCompositionHandle(
    val composition: ServerApplicationComposition,
    private val closeAction: () -> Unit
) : f95gm.server.kernel.LifecycleResource {
    private val closeGuard = f95gm.server.kernel.CloseGuard()
    override val isClosed: Boolean get() = closeGuard.isClosed
    override fun close() = closeGuard.closeOnce(closeAction)
}
