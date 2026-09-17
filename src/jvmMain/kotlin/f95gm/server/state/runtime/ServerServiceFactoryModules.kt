package f95gm.server.state.runtime

import f95gm.server.config.ServerConfig
import f95gm.server.http.request.RequestMetrics
import f95gm.server.http.static.StaticResourceCache
import f95gm.server.persistence.ServerRepositories
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.routes.catalog.CatalogApplicationService
import f95gm.server.routes.item.ItemApplicationService
import f95gm.server.application.media.MediaApplicationService
import f95gm.server.upstream.media.PinnedMediaFetcher
import f95gm.server.upstream.media.PublicHostCache
import f95gm.server.upstream.media.PublicHostResolver
import f95gm.server.cache.CacheFactory
import f95gm.server.cache.CacheNamespaces
import f95gm.server.cache.CachePolicies
import f95gm.server.routes.ports.*
import f95gm.server.application.*
import f95gm.server.upstream.request.UpstreamHttpClient
import kotlinx.coroutines.CoroutineScope
import f95gm.domain.values.F95ThreadId
import f95gm.server.kernel.ServerClock
import f95gm.server.model.marks.LiveMarkInfo

internal object PersistenceServiceFactory {
    fun create(
        database: TransactionBoundary,
        sessions: SessionRegistry,
        clock: ServerClock,
        mutationService: IdempotencyApplicationService,
        repositories: RepositoryDependency
    ) = ServerPersistenceServices(database, sessions, clock, mutationService, repositories)
}

internal object UpstreamServiceFactory {
    fun create(
        client: UpstreamHttpClient,
        scope: CoroutineScope,
        config: ServerConfig,
        policy: ServerUpstreamRuntimePolicy,
        clock: ServerClock,
        sessions: SessionRegistry,
        database: TransactionBoundary,
        metrics: RequestMetrics
    ) = ServerUpstreamServices(ServerUpstreamDependencies(client, scope, config, policy, clock, sessions, database, metrics))
}

internal object InfrastructureServiceFactory {
    fun create(
        clock: ServerClock,
        requestMetrics: RequestMetrics,
        sessions: SessionRegistry,
        activeRequests: ActiveRequestTracker
    ) = RequestMonitoringInfrastructureServices(clock, requestMetrics, sessions, activeRequests)
}

internal object RouteServiceFactory {
    fun create(
        config: ServerConfig,
        clock: ServerClock,
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
        catalogDependencies: UpstreamRouteDependencies,
        itemDependencies: UpstreamRouteDependencies,
        constructionRollback: StartupRollbackStack
    ): ServerRouteServices {
        val publicHostCache = CacheFactory.create<String, Boolean>(
            CachePolicies.PUBLIC_HOST, clock, namespaceOf = { CacheNamespaces.PUBLIC_HOST }
        )
        val publicHostResolver = PublicHostResolver(object : PublicHostCache {
            override fun getOrPut(host: String, factory: () -> Boolean): Boolean = publicHostCache.getOrPut(host, factory)
            override fun clear() = publicHostCache.clear()
        }).also { constructionRollback.register("public-host-resolver", it) }
        val mediaFetcher = PinnedMediaFetcher(
            hostResolver = publicHostResolver,
            connectTimeoutMillis = config.timeouts.connectMillis.value.toInt(),
            readTimeoutMillis = config.timeouts.socketMillis.value.toInt()
        )
        val liveMarkCache = CacheFactory.createManaged<F95ThreadId, LiveMarkInfo>(
            CachePolicies.LIVE_MARK, clock, namespaceOf = { CacheNamespaces.LIVE_MARK }
        ).also { constructionRollback.register("live-mark-cache", it) }
        val staticCache = StaticResourceCache(config.staticDirectoryPath)
            .also { constructionRollback.register("static-resource-cache", it) }
        return ServerRouteServices(
            staticCache = staticCache,
            publicHostResolver = publicHostResolver,
            mediaFetcher = mediaFetcher,
            mediaService = MediaApplicationService(
                publicHostResolver,
                mediaFetcher,
                config.upstreamEndpoints.baseUrl,
                ioDispatcher
            ),
            catalogService = CatalogApplicationService(catalogDependencies),
            itemService = ItemApplicationService(itemDependencies),
            liveMarkCacheResource = liveMarkCache,
            liveMarkCacheAccess = liveMarkCache,
            liveMarkCacheStatistics = liveMarkCache
        )
    }
}
