package f95gm.server.app

import f95gm.server.state.runtime.ServerApplicationComposition
import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource

/** Runtime lifecycle handle for the application composition owned by the composition root. */
internal class ServerCompositionHandle(
    val composition: ServerApplicationComposition,
    private val closeAction: () -> Unit
) : LifecycleResource {
    private val closeGuard = CloseGuard()
    override val isClosed: Boolean get() = closeGuard.isClosed
    override fun close() = closeGuard.closeOnce(closeAction)
}
