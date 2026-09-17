package f95gm.server.state.runtime

import f95gm.server.application.media.MediaApplicationService
import f95gm.server.application.media.MediaFetcher
import f95gm.server.upstream.media.PublicHostResolver
import f95gm.server.application.port.RouteApplicationServices

import f95gm.server.application.SessionRegistry
import f95gm.server.application.SessionService
import f95gm.server.cache.CacheAccess
import f95gm.server.config.Bytes
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.config.upstream.UpstreamEndpointConfig
import f95gm.server.routes.catalog.CatalogApplicationService
import f95gm.server.routes.catalog.CatalogOptionsApplicationService
import f95gm.server.routes.catalog.CatalogRouteDependencies
import f95gm.server.routes.ports.*
import f95gm.server.routes.filters.FilterRouteDependencies
import f95gm.server.routes.item.*
import f95gm.server.routes.marks.live.MarkRouteDependencies
import f95gm.server.routes.session.*
import f95gm.server.upstream.request.UpstreamRequester
import f95gm.server.kernel.ServerClock
import kotlinx.coroutines.CoroutineDispatcher
import f95gm.domain.values.F95ThreadId

private class ServerSessionInfrastructure(
    private val upstreamPort: UpstreamRouteDependencies,
    override val sessions: SessionRegistry,
    override val sessionService: SessionService
) : SessionRouteDependencies, UpstreamRouteDependencies by upstreamPort

internal class ServerMediaInfrastructure(
    private val upstreamPort: UpstreamRouteDependencies,
    override val mediaResourcePolicy: MediaResourcePolicy,
    override val publicHostResolver: PublicHostResolver,
    override val mediaFetcher: MediaFetcher,
    override val mediaService: MediaApplicationService,
    override val ioDispatcher: CoroutineDispatcher
) : MediaRouteDependencies, UpstreamRouteDependencies by upstreamPort

internal data class RoutePolicyDependencies(
    val maxRequestBodyBytes: Bytes,
    val catalogRequestPolicy: CatalogRequestPolicy,
    val filterRequestPolicy: FilterRequestPolicy,
    val mediaResourcePolicy: MediaResourcePolicy
)

internal data class RouteSessionDependencies(
    val sessions: SessionRegistry,
    val sessionService: SessionService
)

internal data class RouteUpstreamDependencies(
    val clock: ServerClock,
    val upstreamEndpoints: UpstreamEndpointConfig,
    val upstream: UpstreamRequester,
    val upstreamLoginClient: UpstreamLoginClient
)

internal class ServerRouteInfrastructureDependencies(
    private val policies: RoutePolicyDependencies,
    private val sessionDependencies: RouteSessionDependencies,
    private val upstreamDependencies: RouteUpstreamDependencies,
    override val applicationServices: RouteApplicationServices,
    private val rejectIfClosed: () -> Unit = {}
) : SessionRouteDependencies, LoginApplicationDependencies, ApplicationRouteDependencies {
    private val upstreamPort = ServerUpstreamRoutePort(
        upstreamDependencies.clock,
        upstreamDependencies.upstreamEndpoints,
        upstreamDependencies.upstream,
        upstreamDependencies.upstreamLoginClient
    )
    override val maxRequestBodyBytes: Bytes get() = policies.maxRequestBodyBytes
    override val catalogRequestPolicy: CatalogRequestPolicy get() = policies.catalogRequestPolicy
    override val filterRequestPolicy: FilterRequestPolicy get() = policies.filterRequestPolicy
    val mediaResourcePolicy: MediaResourcePolicy get() = policies.mediaResourcePolicy
    override val clock: ServerClock get() = upstreamPort.clock
    override val upstreamEndpoints: UpstreamEndpointConfig get() = upstreamPort.upstreamEndpoints
    override val upstream: UpstreamRequester get() = rejectIfClosed().let { upstreamPort.upstream }
    override val upstreamLoginClient: UpstreamLoginClient get() = upstreamPort.upstreamLoginClient
    override val sessions: SessionRegistry get() = sessionsPort.sessions
    override val sessionService: SessionService get() = sessionsPort.sessionService
    private val sessionsPort = ServerSessionInfrastructure(
        upstreamPort,
        sessionDependencies.sessions,
        sessionDependencies.sessionService
    )
}

internal class ServerCatalogRoutePort(
    private val infrastructure: ServerRouteInfrastructureDependencies,
    override val catalogService: CatalogApplicationService,
    override val catalogOptionsService: CatalogOptionsApplicationService
) : CatalogRouteDependencies, UpstreamRouteDependencies by infrastructure {
    override val catalogRequestPolicy: CatalogRequestPolicy get() = infrastructure.catalogRequestPolicy
}

internal class ServerFilterRoutePort(
    private val infrastructure: ServerRouteInfrastructureDependencies
) : FilterRouteDependencies,
    SessionRouteDependencies by infrastructure,
    ApplicationRouteDependencies by infrastructure,
    ClockRouteDependencies by infrastructure {
    override val clock: ServerClock get() = infrastructure.clock
}

internal class ServerLoginRoutePort(
    private val infrastructure: ServerRouteInfrastructureDependencies,
    override val loginService: LoginApplicationService
) : LoginRouteDependencies, LoginApplicationDependencies by infrastructure {
    override val maxRequestBodyBytes: Bytes get() = infrastructure.maxRequestBodyBytes
}

internal class ServerItemRoutePort(
    private val infrastructure: ServerRouteInfrastructureDependencies,
    override val itemService: ItemApplicationService
) : ItemRouteDependencies, UpstreamRouteDependencies by infrastructure

internal class ServerMarkRoutePort(
    infrastructure: ServerRouteInfrastructureDependencies,
    override val liveMarkCache: CacheAccess<F95ThreadId, LiveMarkInfo>
) : MarkRouteDependencies, SessionRouteDependencies by infrastructure, ApplicationRouteDependencies by infrastructure
