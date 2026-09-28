package f95gm.server.kernel

/** Adapts a synchronous callback to the explicit lifecycle contract. */
internal class LifecycleCallbackResource(private val closeAction: () -> Unit) : LifecycleResource {
    private val closeGuard = CloseGuard()

    override val isClosed: Boolean get() = closeGuard.isClosed
    override fun close() = closeGuard.closeOnce(closeAction)
}
