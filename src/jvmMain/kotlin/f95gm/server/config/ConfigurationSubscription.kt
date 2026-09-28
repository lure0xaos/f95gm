package f95gm.server.config

import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource

internal class ConfigurationSubscription internal constructor(private val unsubscribe: () -> Unit) : LifecycleResource {
    private val closed = CloseGuard()
    override val isClosed: Boolean get() = closed.isClosed
    override fun close() = closed.closeOnce(unsubscribe)
}
