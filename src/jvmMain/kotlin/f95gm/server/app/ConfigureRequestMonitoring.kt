package f95gm.server.app

import f95gm.server.config.Bytes
import f95gm.server.observability.*
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.config.ServerConfigurationState
import f95gm.server.http.request.*
import f95gm.server.http.request.ratelimit.RateLimitPolicy
import f95gm.server.state.runtime.ServerApplicationComposition
import io.ktor.server.application.*

internal data class RequestMonitoringConfiguration(
    val maxRequestBodyBytes: Bytes,
    val rateLimitPolicy: RateLimitPolicy,
    val maxRateLimitBuckets: RateLimitBucketLimit,
    val requestIdGenerator: RequestIdGenerator = SecureRequestIdentifiers.requestIds,
    val configurationState: ServerConfigurationState? = null
)

internal fun Application.installRequestMonitoring(
    shutdownController: () -> ServerShutdownController,
    routeContext: ServerApplicationComposition,
    configuration: RequestMonitoringConfiguration
) = installRequestMonitoringWithPolicy(
    shutdownController, routeContext, configuration
)

private fun Application.installRequestMonitoringWithPolicy(
    shutdownController: () -> ServerShutdownController,
    routeContext: ServerApplicationComposition,
    configuration: RequestMonitoringConfiguration
) {
    val infrastructure = routeContext.infrastructure
    val monitoring = RequestMonitoringRuntime(configuration, infrastructure)
    val interceptor = RequestMonitoringInterceptor(shutdownController, infrastructure, configuration, monitoring)
    monitor.subscribe(ApplicationStopped) {
        monitoring.close()
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        interceptor.intercept(call, ::finish) { proceed() }
    }
}
