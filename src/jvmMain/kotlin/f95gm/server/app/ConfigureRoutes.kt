package f95gm.server.app

import f95gm.server.config.Bytes
import f95gm.server.config.constants.ServerConstants
import f95gm.server.desktop.DesktopController
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.request.RequestIdGenerator
import f95gm.server.http.request.SecureRequestIdentifiers
import f95gm.server.http.request.ShutdownToken
import f95gm.server.http.request.ratelimit.RateLimitPolicy
import f95gm.server.state.runtime.ServerContext
import f95gm.server.state.runtime.installServerContext
import io.ktor.server.application.*
import io.ktor.server.routing.*

@JvmInline
internal value class RateLimitBucketLimit(val value: Int)

internal data class RouteConfiguration(
    val staticDirectory: String? = null,
    val maxRequestBodyBytes: Bytes = Bytes(ServerConstants.MAX_REQUEST_BODY_BYTES),
    val rateLimitPolicy: RateLimitPolicy = RateLimitPolicy.fromApiRequestsPerMinute(
        ServerConstants.API_REQUESTS_PER_MINUTE,
        ServerConstants.RATE_LIMIT_WINDOW_MILLIS
    ),
    val maxRateLimitBuckets: RateLimitBucketLimit = RateLimitBucketLimit(ServerConstants.MAX_RATE_LIMIT_BUCKETS),
    val requestIdGenerator: RequestIdGenerator = SecureRequestIdentifiers.requestIds
)

internal fun Application.configureRoutes(
    shutdownToken: ShutdownToken,
    shutdownController: () -> ServerShutdownController,
    desktop: () -> DesktopController?,
    routeConfiguration: RouteConfiguration,
    context: ServerContext
) = configureRoutesImplementation(shutdownToken, shutdownController, desktop, routeConfiguration, context)

private fun Application.configureRoutesImplementation(
    shutdownToken: ShutdownToken,
    shutdownController: () -> ServerShutdownController,
    desktop: () -> DesktopController?,
    routeConfiguration: RouteConfiguration,
    context: ServerContext,
) {
    installServerContext(context)
    installRequestMonitoring(
        shutdownController,
        context,
        RequestMonitoringConfiguration(
            maxRequestBodyBytes = routeConfiguration.maxRequestBodyBytes,
            rateLimitPolicy = routeConfiguration.rateLimitPolicy,
            maxRateLimitBuckets = routeConfiguration.maxRateLimitBuckets,
            requestIdGenerator = routeConfiguration.requestIdGenerator
        )
    )
    routing {
        configureHealthRoute(shutdownController, desktop, context)
        configureShutdownRoute(shutdownToken, shutdownController)
        configureApiRoutes()
        configureStaticRoutes(shutdownToken, context.staticCache)
    }
}
