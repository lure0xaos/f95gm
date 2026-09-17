package f95gm.server.state.runtime

import f95gm.server.kernel.ServerClock

import f95gm.server.config.ServerConfigurationState
import f95gm.server.application.SessionRegistry
import kotlin.time.Duration
import f95gm.server.kernel.AwaitableLifecycleResource

/** Owns the ordered lifecycle graph for the composed runtime. */
internal class ServerRuntimeLifecycle(
    resources: ServerResources,
    upstream: ServerUpstreamServices,
    routes: ServerRouteServices,
    sessions: SessionRegistry,
    clock: ServerClock,
    configuration: ServerConfigurationState,
    shutdownTimeout: Duration
) : AwaitableLifecycleResource {
    private val controller = ServerLifecycleController(
        ServerLifecycle(shutdownTimeout), resources, upstream, routes, sessions, clock, configuration
    )

    override val isClosed: Boolean get() = controller.isClosed
    val terminalFailure: Throwable? get() = controller.terminalFailure
    override fun close() = controller.close()
    override suspend fun closeAndJoin() = controller.closeAndJoin()
}
