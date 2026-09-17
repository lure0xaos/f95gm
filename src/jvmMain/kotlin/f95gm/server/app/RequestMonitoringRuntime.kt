package f95gm.server.app

import f95gm.server.http.request.ApiEndpointResolver
import f95gm.server.http.request.RequestMetrics
import f95gm.server.state.runtime.RequestMonitoringInfrastructure
import f95gm.server.kernel.LifecycleResource

/** Independently testable monitoring components assembled at the application boundary. */
internal class RequestMonitoringRuntime(
    configuration: RequestMonitoringConfiguration,
    infrastructure: RequestMonitoringInfrastructure
) : LifecycleResource {
    val resources = RequestMonitoringResources(configuration, infrastructure.clock, configuration.configurationState)
    val metrics: RequestMetrics = infrastructure.requestMetrics
    val pipeline = RequestPipeline(
        infrastructure.clock,
        metrics,
        infrastructure.activeRequests,
        RequestPipelineConfiguration.DEFAULT
    )
    val endpointResolver = ApiEndpointResolver()

    override val isClosed: Boolean get() = resources.isClosed
    override fun close() = resources.close()
}
