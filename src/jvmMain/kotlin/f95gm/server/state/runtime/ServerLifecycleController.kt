package f95gm.server.state.runtime

import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResourceName
import f95gm.server.kernel.LifecycleResources

import f95gm.server.kernel.ServerClock
import f95gm.server.kernel.FailureBoundary
import f95gm.server.kernel.AwaitableLifecycleResource
import f95gm.server.kernel.LifecycleCallbackResource

import f95gm.server.config.ServerConfigurationState
import f95gm.server.application.SessionRegistry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Owns ordered shutdown and the background work attached to the server lifecycle. */
internal class ServerLifecycleController(
    private val lifecycle: ServerLifecycle,
    private val resources: ServerResources,
    upstream: ServerUpstreamServices,
    routes: ServerRouteServices,
    sessions: SessionRegistry,
    clock: ServerClock,
    configuration: ServerConfigurationState
) : AwaitableLifecycleResource {
    private val sessionEvictionJob: Job

    init {
        lifecycle.register(
            LifecycleResources.CONFIGURATION,
            configuration,
            failureBoundary = FailureBoundary.LIFECYCLE
        )
        lifecycle.register(
            LifecycleResources.RESOURCES,
            resources,
            failureBoundary = FailureBoundary.PERSISTENCE
        )
        lifecycle.registerWithDependencies(
            LifecycleResources.UPSTREAM,
            upstream,
            listOf(LifecycleResources.RESOURCES),
            FailureBoundary.UPSTREAM
        )
        lifecycle.registerWithDependencies(LifecycleResources.ROUTES, routes, listOf(LifecycleResources.RESOURCES, LifecycleResources.UPSTREAM))
        sessionEvictionJob = resources.launchOwned {
            while (isActive) {
                val snapshot = configuration.current().value
                val idle = snapshot.timeouts.sessionIdleMillis.value.milliseconds
                delay(idle.coerceAtMost(60.seconds))
                sessions.evictIdle(clock.monotonicMillisTimestamp(), SessionRegistry.SessionIdleTimeout(idle))
            }
        }
        lifecycle.registerWithDependencies(
            LifecycleResources.SESSION_EVICTION,
            LifecycleCallbackResource { sessionEvictionJob.cancel() },
            listOf(LifecycleResources.RESOURCES)
        )
    }

    override val isClosed: Boolean get() = lifecycle.isClosed
    val terminalFailure: Throwable? get() = lifecycle.terminalFailure
    override fun close() = lifecycle.close()

    override suspend fun closeAndJoin() = lifecycle.closeAndJoin()
}

