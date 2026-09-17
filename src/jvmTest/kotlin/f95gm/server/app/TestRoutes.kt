package f95gm.server.app

import f95gm.server.config.Bytes
import f95gm.server.desktop.DesktopLifecycle
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.request.ShutdownToken
import f95gm.server.http.request.ratelimit.RateLimitPolicy
import f95gm.server.state.runtime.testServerContext
import f95gm.server.state.runtime.ServerContext
import io.ktor.server.application.*

/** Test-only adapter; production startup accepts [ServerApplicationComposition] directly. */
internal fun Application.configureRoutes(
    shutdownToken: ShutdownToken,
    shutdownController: () -> ServerShutdownController,
    routeConfiguration: RouteConfiguration,
    context: ServerContext
): Unit = configureRoutes(
    shutdownToken,
    shutdownController,
    routeConfiguration,
    context.applicationComposition,
    ServerHealthState()
)

internal fun Application.configureTestRoutes(
    shutdownToken: String,
    shutdownController: () -> ServerShutdownController,
    desktop: () -> DesktopLifecycle,
    maxRequestBodyBytes: Long = f95gm.server.config.RequestLimits.MAX_BODY_BYTES,
    apiRequestsPerMinute: Int = f95gm.server.config.RateLimitDefaults.API_REQUESTS_PER_MINUTE,
    rateLimitWindowMillis: Long = f95gm.server.config.RateLimitDefaults.WINDOW_MILLIS,
    maxRateLimitBuckets: Int = f95gm.server.config.RateLimitDefaults.MAX_BUCKETS
): Unit {
    val context = testServerContext()
    monitor.subscribe(ApplicationStopped) { context.close() }
    configureRoutes(
        ShutdownToken(shutdownToken),
        shutdownController,
        RouteConfiguration(
            maxRequestBodyBytes = Bytes(maxRequestBodyBytes),
            rateLimitPolicy = RateLimitPolicy.configured(
                apiRequestsPerMinute,
                (apiRequestsPerMinute / 2).coerceAtLeast(1),
                rateLimitWindowMillis
            ),
            maxRateLimitBuckets = RateLimitBucketLimit(maxRateLimitBuckets)
        ),
        context
    )
}
