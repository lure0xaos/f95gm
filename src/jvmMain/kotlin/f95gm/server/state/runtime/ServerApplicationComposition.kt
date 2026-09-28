package f95gm.server.state.runtime

import f95gm.server.kernel.ServerClock
import f95gm.server.application.SessionService

import f95gm.server.config.ServerConfigurationState
import f95gm.server.cache.CacheStatisticsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import kotlin.coroutines.CoroutineContext
import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource

/** Child scope whose cancellation cannot cancel its owning resource. */
internal class OwnedLifecycleScope private constructor(
    override val coroutineContext: CoroutineContext
) : CoroutineScope, LifecycleResource {
    private val closeGuard = CloseGuard()
    companion object {
        fun childOf(parent: CoroutineScope): OwnedLifecycleScope {
            val parentJob = checkNotNull(parent.coroutineContext[Job])
            return OwnedLifecycleScope(parent.coroutineContext.minusKey(Job) + SupervisorJob(parentJob))
        }
    }

    val isActive: Boolean get() = coroutineContext[Job]?.isActive == true
    override val isClosed: Boolean get() = closeGuard.isClosed
    override fun close() = closeGuard.closeOnce { coroutineContext[Job]?.cancel() }
}

/** Narrow lifecycle handle used at composition boundaries. */
internal class LifecycleScopeHandle(private val context: kotlin.coroutines.CoroutineContext) {
    private val scope = CoroutineScope(context)
    val isActive: Boolean get() = context[kotlinx.coroutines.Job]?.isActive == true
    fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch(block = block)
    fun launch(start: CoroutineStart, block: suspend CoroutineScope.() -> Unit): Job =
        scope.launch(start = start, block = block)
    fun cancel() { context[Job]?.cancel() }

    companion object {
        fun from(scope: CoroutineScope): LifecycleScopeHandle = LifecycleScopeHandle(scope.coroutineContext)
        fun test(dispatcher: CoroutineDispatcher = Dispatchers.Default): LifecycleScopeHandle =
            LifecycleScopeHandle(SupervisorJob() + dispatcher)
    }
}

internal data class RuntimeComposition(
    override val clock: ServerClock,
    override val requestMetrics: f95gm.server.http.request.RequestMetrics,
    override val activeRequests: ActiveRequestTracker,
    override val lifecycleScope: LifecycleScopeHandle,
    override val configurationState: ServerConfigurationState,
    override val cacheStatisticsProvider: CacheStatisticsProvider
): RuntimeCompositionPort

internal interface RuntimeCompositionPort {
    val clock: ServerClock
    val requestMetrics: f95gm.server.http.request.RequestMetrics
    val activeRequests: ActiveRequestTracker
    val lifecycleScope: LifecycleScopeHandle
    val configurationState: ServerConfigurationState
    val cacheStatisticsProvider: CacheStatisticsProvider
}

internal data class PersistenceComposition(override val sessionService: SessionService) : PersistenceCompositionPort

internal interface PersistenceCompositionPort {
    val sessionService: SessionService
}

internal data class RouteComposition(
    override val services: ServerRouteServices,
    override val dependencies: RouteDependencyPorts
) : RouteCompositionPort

internal interface RouteCompositionPort {
    val services: ServerRouteServices
    val dependencies: RouteDependencyPorts
}

/** Stable, narrow composition slices used by startup and lifecycle orchestration. */
internal data class ServerApplicationComposition(
    val runtime: RuntimeCompositionPort,
    val infrastructure: RequestMonitoringInfrastructure,
    val persistence: PersistenceCompositionPort,
    val routes: RouteCompositionPort
)
