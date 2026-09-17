package f95gm.server.app

import f95gm.server.config.Bytes
import f95gm.server.config.RateLimitDefaults
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.request.RequestIdGenerator
import f95gm.server.http.request.SecureRequestIdentifiers
import f95gm.server.http.request.ShutdownToken
import f95gm.server.http.request.ratelimit.RateLimitPolicy
import f95gm.server.state.runtime.ServerApplicationComposition
import f95gm.server.state.runtime.installRouteDependencies
import io.ktor.server.application.*
import io.ktor.server.routing.*

@JvmInline
internal value class RateLimitBucketLimit(val value: Int)

internal data class RouteConfiguration(
    val maxRequestBodyBytes: Bytes = ServerOperationalDefaults.request.maxRequestBodyBytes,
    val rateLimitPolicy: RateLimitPolicy = RateLimitPolicy.fromApiRequestsPerMinute(
        RateLimitDefaults.API_REQUESTS_PER_MINUTE,
        RateLimitDefaults.WINDOW_MILLIS
    ),
    val maxRateLimitBuckets: RateLimitBucketLimit = ServerOperationalDefaults.request.maxRateLimitBuckets,
    val requestIdGenerator: RequestIdGenerator = SecureRequestIdentifiers.requestIds
)

internal fun Application.configureRoutes(
    shutdownToken: ShutdownToken,
    shutdownController: () -> ServerShutdownController,
    routeConfiguration: RouteConfiguration,
    composition: ServerApplicationComposition,
    healthState: ServerHealthState
) = configureRoutesImplementation(shutdownToken, shutdownController, routeConfiguration, composition, healthState)

private fun Application.configureRoutesImplementation(
    shutdownToken: ShutdownToken,
    shutdownController: () -> ServerShutdownController,
    routeConfiguration: RouteConfiguration,
    composition: ServerApplicationComposition,
    healthState: ServerHealthState,
) {
    installRouteDependencies(composition.routes.dependencies)
    installRequestMonitoring(
        shutdownController,
        composition,
        RequestMonitoringConfiguration(
            maxRequestBodyBytes = routeConfiguration.maxRequestBodyBytes,
            rateLimitPolicy = routeConfiguration.rateLimitPolicy,
            maxRateLimitBuckets = routeConfiguration.maxRateLimitBuckets,
            requestIdGenerator = routeConfiguration.requestIdGenerator,
            configurationState = composition.runtime.configurationState
        )
    )
    routing {
        configureHealthRoute(
            shutdownController,
            composition.infrastructure.requestMetrics,
            composition.runtime.cacheStatisticsProvider,
            healthState
        )
        configureShutdownRoute(shutdownToken, shutdownController)
        configureApiRoutes()
        configureStaticRoutes(shutdownToken, composition.routes.services.staticCache)
    }
}
