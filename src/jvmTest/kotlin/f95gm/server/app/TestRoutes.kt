package f95gm.server.app

import f95gm.server.config.Bytes
import f95gm.server.config.constants.ServerConstants
import f95gm.server.desktop.DesktopController
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.request.ShutdownToken
import f95gm.server.http.request.ratelimit.RateLimitPolicy
import f95gm.server.state.runtime.testServerContext
import io.ktor.server.application.*

internal fun Application.configureTestRoutes(
    shutdownToken: String,
    shutdownController: () -> ServerShutdownController,
    desktop: () -> DesktopController?,
    staticDirectory: String? = null,
    maxRequestBodyBytes: Long = ServerConstants.MAX_REQUEST_BODY_BYTES,
    apiRequestsPerMinute: Int = ServerConstants.API_REQUESTS_PER_MINUTE,
    rateLimitWindowMillis: Long = ServerConstants.RATE_LIMIT_WINDOW_MILLIS,
    maxRateLimitBuckets: Int = ServerConstants.MAX_RATE_LIMIT_BUCKETS
) {
    val context = testServerContext()
    monitor.subscribe(ApplicationStopped) { context.close() }
    configureRoutes(
        ShutdownToken(shutdownToken),
        shutdownController,
        desktop,
        RouteConfiguration(
            staticDirectory = staticDirectory,
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
