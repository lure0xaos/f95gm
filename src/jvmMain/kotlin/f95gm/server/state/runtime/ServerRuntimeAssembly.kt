package f95gm.server.state.runtime

import f95gm.server.kernel.ServerInvariantViolation

import f95gm.server.config.ServerConfig
import f95gm.server.kernel.ServerClock
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.persistence.ServerRepositories
import f95gm.server.routes.session.UpstreamLoginClientImpl
import f95gm.server.routes.ports.*
import f95gm.server.routes.ports.UpstreamLoginClient
import f95gm.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration.Companion.milliseconds
import f95gm.server.cache.CacheStatisticsProvider
import f95gm.server.cache.CacheStatisticsSource
import f95gm.server.cache.CompositeCacheStatisticsProvider
import f95gm.server.cache.CacheNamespaces

/** Construction-only slice of the runtime; lifecycle control remains in the facade. */
internal class ServerRuntimeAssembly(
    config: ServerConfig,
    clock: ServerClock,
    dependencies: ServerRuntimeDependencies,
    isClosed: () -> Boolean,
    private val constructionRollback: StartupRollbackStack
) {
    private val observabilityModule = ServerRuntimeObservabilityModule.create(config)
    private val requestMetrics = observabilityModule.requestMetrics
    val resources = ServerResources(
        ServerHttpPolicies.from(config).clientTimeouts,
        dependencies.dispatcher,
        dependencies.httpClient,
        config.timeouts.shutdownMillis.value.milliseconds
    ).also { constructionRollback.register("runtime-resources", it) }
    val registrySnapshots = RegistrySnapshotCoordinator()
    val sessions = SessionRegistry(clock, config.limits.maxSessions.value, registrySnapshots)
    val activeRequests = ActiveRequestTracker()
    val infrastructureServices = InfrastructureServiceFactory.create(clock, requestMetrics, sessions, activeRequests)
    val idempotency = IdempotencyRegistry(clock = clock, registrySnapshots = registrySnapshots)
    val mutationService = IdempotencyApplicationService(idempotency)
    val persistenceServices = PersistenceServiceFactory.create(
        dependencies.database, sessions, clock, mutationService, dependencies.repositories
    )
    val lifecycleScope = resources.createChildScope()
    val upstreamServices = UpstreamServiceFactory.create(
        resources.upstreamHttpClient,
        lifecycleScope,
        config,
        ServerUpstreamRuntimePolicy(
            ServerUpstreamPolicies.from(config).request,
            config.traffic.circuitBreakerOpenMillis.value.milliseconds,
            config.traffic.maxCircuitBreakerEntries.value,
            config.limits.maxUpstreamConcurrentRequests.value
        ),
        clock,
        sessions,
        dependencies.database,
        requestMetrics
    ).also { constructionRollback.register("upstream-services", it) }
    val upstreamLoginClient = UpstreamLoginClientImpl()
    val routeComposition: RouteComposition = ServerRouteCompositionBuilder(
        config,
        clock,
        sessions,
        persistenceServices,
        upstreamServices,
        upstreamLoginClient,
        ioDispatcher = dependencies.dispatcher,
        constructionRollback = constructionRollback,
        rejectIfClosed = { if (isClosed()) throw ServerInvariantViolation("Server runtime is closed") }
    ).build()

    val cacheStatsProvider: CacheStatisticsProvider = CompositeCacheStatisticsProvider(
        listOf(
            CacheStatisticsSource(CacheNamespaces.LIVE_MARK, routeComposition.services.liveMarkCacheStatistics),
            CacheStatisticsSource(CacheNamespaces.CLASSPATH, routeComposition.services.staticCache)
        ),
        qualifyNamespaces = false
    )

    val observability: ServerRuntimeObservability = ServerRuntimeObservability(
        observabilityModule.requestMetrics,
        cacheStatsProvider
    )
}
