package f95gm.server.state.runtime

import f95gm.server.kernel.ServerClock

import f95gm.server.config.ServerConfig
import f95gm.server.config.CatalogLimits
import f95gm.server.routes.ports.UpstreamLoginClient
import f95gm.server.routes.session.LoginApplicationService
import f95gm.server.routes.catalog.CatalogOptionsApplicationService
import f95gm.server.routes.ports.*
import f95gm.server.application.SessionRegistry
import kotlinx.coroutines.CoroutineDispatcher

/** Builds the route graph in one explicit boundary, keeping cyclic service wiring out of the runtime root. */
internal class ServerRouteCompositionBuilder(
    private val config: ServerConfig,
    private val clock: ServerClock,
    private val sessions: SessionRegistry,
    private val persistence: ServerPersistenceServices,
    private val upstream: ServerUpstreamServices,
    private val upstreamLoginClient: UpstreamLoginClient,
    private val ioDispatcher: CoroutineDispatcher,
    private val constructionRollback: StartupRollbackStack,
    private val rejectIfClosed: () -> Unit = {}
) {
    fun build(): RouteComposition {
        val infrastructure = ServerRouteInfrastructureDependencies(
            policies = RoutePolicyDependencies(
                maxRequestBodyBytes = config.limits.maxRequestBodyBytes,
                mediaResourcePolicy = MediaResourcePolicy(
                    maxResponseBytes = config.limits.maxMediaResponseBytes,
                    downloadBufferBytes = config.limits.mediaDownloadBufferBytes
                ),
                catalogRequestPolicy = CatalogRequestPolicy(
                    defaultRows = CatalogLimits.PAGE_SIZE,
                    allowedRows = CatalogLimits.PAGE_SIZES,
                    maxPage = config.limits.maxCatalogPage,
                    maxTags = config.limits.maxCatalogTags,
                    maxTextLength = config.limits.maxCatalogTextLength
                ),
                filterRequestPolicy = FilterRequestPolicy(
                    maxImportedQuickLinks = config.limits.maxImportedQuickLinks,
                    maxFilterIdLength = config.limits.maxFilterIdLength
                )
            ),
            sessionDependencies = RouteSessionDependencies(
                sessions = sessions,
                sessionService = persistence.sessionService
            ),
            upstreamDependencies = RouteUpstreamDependencies(
                clock = clock,
                upstreamEndpoints = config.upstreamEndpoints,
                upstream = upstream.gateway,
                upstreamLoginClient = upstreamLoginClient
            ),
            applicationServices = persistence.applicationServices,
            rejectIfClosed = rejectIfClosed
        )
        val loginService = LoginApplicationService(infrastructure)
        val catalogOptionsService = CatalogOptionsApplicationService(infrastructure)
        val services = RouteServiceFactory.create(
            config, clock, ioDispatcher, infrastructure, infrastructure, constructionRollback
        )
        val routePorts = createRouteDependencyPorts(
            infrastructure = infrastructure,
            loginService = loginService,
            catalogService = services.catalogService,
            catalogOptionsService = catalogOptionsService,
            itemService = services.itemService,
            publicHostResolver = services.publicHostResolver,
            mediaFetcher = services.mediaFetcher,
            mediaService = services.mediaService,
            liveMarkCache = services.liveMarkCacheAccess
            ,ioDispatcher = ioDispatcher
        )
        return RouteComposition(services, routePorts)
    }
}
