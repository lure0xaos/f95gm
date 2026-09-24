package f95gm.server.app

import f95gm.server.desktop.DesktopController
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.state.runtime.testServerContext
import io.ktor.server.application.*

internal fun Application.configureTestRoutes(
    shutdownToken: String,
    shutdownController: () -> ServerShutdownController,
    desktop: () -> DesktopController?,
    staticDirectory: String? = null,
    maxRequestBodyBytes: Long = f95gm.server.config.constants.ServerConstants.MAX_REQUEST_BODY_BYTES,
    apiRequestsPerMinute: Int = f95gm.server.config.constants.ServerConstants.API_REQUESTS_PER_MINUTE,
    rateLimitWindowMillis: Long = f95gm.server.config.constants.ServerConstants.RATE_LIMIT_WINDOW_MILLIS,
    maxRateLimitBuckets: Int = f95gm.server.config.constants.ServerConstants.MAX_RATE_LIMIT_BUCKETS
) {
    val context = testServerContext()
    monitor.subscribe(ApplicationStopped) { context.close() }
    configureRoutes(
        f95gm.server.http.request.ShutdownToken(shutdownToken),
        shutdownController,
        desktop,
        RouteConfiguration(
            staticDirectory = staticDirectory,
            maxRequestBodyBytes = f95gm.server.config.Bytes(maxRequestBodyBytes),
            rateLimitPolicy = f95gm.server.http.request.ratelimit.RateLimitPolicy.configured(
                apiRequestsPerMinute,
                (apiRequestsPerMinute / 2).coerceAtLeast(1),
                rateLimitWindowMillis
            ),
            maxRateLimitBuckets = RateLimitBucketLimit(maxRateLimitBuckets)
        ),
        context
    )
}
