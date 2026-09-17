package f95gm.server.app

import f95gm.server.config.Bytes
import f95gm.server.observability.*
import f95gm.server.config.constants.HttpStatusCatalog
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.request.*
import f95gm.server.http.request.ratelimit.RateLimitPolicy
import f95gm.server.http.response.ApiCsrfException
import f95gm.server.http.response.ApiRequestTooLargeException
import f95gm.server.state.runtime.RequestAdmissionInfrastructure
import io.ktor.server.application.*
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond

internal suspend fun ApplicationCall.admitRequest(
    shutdownController: () -> ServerShutdownController,
    infrastructure: RequestAdmissionInfrastructure,
    resources: RequestMonitoringResources,
    endpointResolver: ApiEndpointResolver,
    maxRequestBodyBytes: Bytes,
    complete: () -> Unit,
    rejected: () -> Unit
): AuthenticationResult {
    if (shutdownController().isDraining && !isPublicDuringDrain()) {
        respond(HttpStatusCatalog.SERVICE_UNAVAILABLE)
        complete()
        return AuthenticationResult.ResponseCompleted
    }

    val requestPath = request.requestPath().value
    val surface = if (endpointResolver.isApiPath(requestPath)) RequestSurface.API else RequestSurface.BROWSER
    val descriptor = endpointResolver.resolve(requestPath, request.httpMethod)
    val endpoint = RequestEndpoint(descriptor?.path?.value ?: requestPath)
    when (resources.originPolicy.evaluate(this, surface, descriptor)) {
        SecurityPolicyDecision.Allowed -> Unit
        is SecurityPolicyDecision.Rejected -> {
            logger.info {
                auditLog(AuditEvent(AuditEventName.CSRF_REJECTED, listOf(logField(StructuredLogKeys.REASON, "origin_policy"))))
            }
            throw ApiCsrfException()
        }
    }
    if (surface == RequestSurface.API) {
        try {
            RequestBodyLimit.enforceContentLength(this, maxRequestBodyBytes.value)
            validateApiContentLength(maxRequestBodyBytes.value)
        } catch (error: ApiRequestTooLargeException) {
            rejected()
            throw error
        }
    }
    resources.rateLimitPolicy.enforce(
        this, surface, descriptor,
        endpoint, infrastructure.sessions, infrastructure.clock, infrastructure.requestMetrics
    )
    return authenticateIfRequired(descriptor, infrastructure.sessions).also {
        if (it == AuthenticationResult.ResponseCompleted) complete()
    }
}
