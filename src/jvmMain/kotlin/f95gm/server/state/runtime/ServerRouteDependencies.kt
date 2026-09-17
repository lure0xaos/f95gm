package f95gm.server.state.runtime

import f95gm.server.cache.CacheAccess
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.routes.catalog.*
import f95gm.server.routes.item.*
import f95gm.server.application.media.MediaApplicationService
import f95gm.server.application.media.MediaFetcher
import f95gm.server.upstream.media.PublicHostResolver
import f95gm.server.routes.session.LoginApplicationService
import f95gm.server.routes.ports.UpstreamRouteDependencies
import f95gm.server.routes.filters.FilterRouteDependencies
import f95gm.server.routes.marks.live.MarkRouteDependencies
import f95gm.domain.values.F95ThreadId


/** Builds the feature ports without retaining a second aggregate dependency object. */
internal fun createRouteDependencyPorts(
    infrastructure: ServerRouteInfrastructureDependencies,
    loginService: LoginApplicationService,
    catalogService: CatalogApplicationService,
    catalogOptionsService: CatalogOptionsApplicationService,
    itemService: ItemApplicationService,
    publicHostResolver: PublicHostResolver,
    mediaFetcher: MediaFetcher,
    mediaService: MediaApplicationService,
    liveMarkCache: CacheAccess<F95ThreadId, LiveMarkInfo>,
    ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
): RouteDependencyPorts {
    val catalogPort = ServerCatalogRoutePort(infrastructure, catalogService, catalogOptionsService)
    val filterPort = ServerFilterRoutePort(infrastructure)
    val loginPort = ServerLoginRoutePort(infrastructure, loginService)
    val itemPort = ServerItemRoutePort(infrastructure, itemService)
    val markPort = ServerMarkRoutePort(infrastructure, liveMarkCache)
    val mediaPort = ServerMediaInfrastructure(
        upstreamPort = infrastructure,
        mediaResourcePolicy = infrastructure.mediaResourcePolicy,
        publicHostResolver = publicHostResolver,
        mediaFetcher = mediaFetcher,
        mediaService = mediaService,
        ioDispatcher = ioDispatcher
    )
    return RouteDependencyPorts(
        application = infrastructure,
        filter = filterPort,
        item = itemPort,
        login = loginPort,
        catalog = catalogPort,
        mark = markPort,
        media = mediaPort,
        session = infrastructure
    )
}
