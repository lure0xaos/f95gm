package f95gm.server.app

import f95gm.server.config.protocol.HttpProtocolConfig
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.request.*
import f95gm.server.state.runtime.RequestMonitoringInfrastructure
import f95gm.server.observability.FailureReporter
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.withContext

/** Request-scoped interceptor; admission, authentication, and completion remain separate services. */
internal class RequestMonitoringInterceptor(
    private val shutdownController: () -> ServerShutdownController,
    private val infrastructure: RequestMonitoringInfrastructure,
    private val configuration: RequestMonitoringConfiguration,
    private val monitoring: RequestMonitoringRuntime
) {
    suspend fun intercept(call: ApplicationCall, complete: () -> Unit, proceed: suspend () -> Unit) {
        call.appendSecurityHeaders()
        val requestId = configuration.requestId(call)
        val requestContext = RequestContext(requestId, call.request.local.remoteHost)
        call.installRequestContext(requestContext)
        call.installRequestScope(RequestScope(requestContext, infrastructure.clock.snapshot(), infrastructure.requestMetrics))
        call.response.headers.append(HttpProtocolConfig.REQUEST_ID_HEADER, requestId.value)
        val state = monitoring.pipeline.start(requestId)
        val requestLease = monitoring.pipeline.enter()
        try {
            FailureReporter.runApplicationBoundary(
                onFailure = { error -> state.recordFailure(RequestFailure.from(error)) }
            ) {
            val requestPath = call.request.requestPath().value
            val descriptor = monitoring.endpointResolver.resolve(requestPath, call.request.httpMethod)
            state.endpoint = RequestEndpoint(descriptor?.path?.value ?: state.endpoint.value)
            when (call.admitRequest(
                shutdownController = shutdownController,
                infrastructure = infrastructure,
                resources = monitoring.resources,
                endpointResolver = monitoring.endpointResolver,
                maxRequestBodyBytes = configuration.maxRequestBodyBytes,
                complete = complete,
                rejected = monitoring.metrics::recordRejected
            )) {
                AuthenticationResult.ResponseCompleted -> {
                    complete()
                    return
                }
                AuthenticationResult.Allowed -> Unit
            }
            withContext(requestContext.asCoroutineContext()) { monitoring.pipeline.execute(call, state, proceed) }
            }
        } finally {
            if (state.phase != RequestLifecyclePhase.COMPLETED) state.complete()
            monitoring.pipeline.finish(call, state)
            requestLease.close()
        }
    }
}
