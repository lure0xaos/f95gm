package f95gm.server.app

import f95gm.domain.api.EndpointDescriptor
import f95gm.domain.api.MutationKind
import f95gm.domain.api.RateLimitScope
import f95gm.server.observability.*
import f95gm.server.http.request.origin.OriginPolicy
import f95gm.server.http.request.origin.OriginValidationCache
import f95gm.server.http.request.ratelimit.RateLimitHeaders
import f95gm.server.http.request.ratelimit.RateLimitPolicy
import f95gm.server.http.request.ratelimit.RequestRateLimiter
import f95gm.server.http.request.RequestEndpoint
import f95gm.server.http.response.ApiRateLimitedException
import f95gm.server.routes.session.browserSession
import f95gm.server.routes.session.BrowserSession
import f95gm.server.kernel.ServerClock
import f95gm.server.application.SessionRegistry
import f95gm.server.http.request.RequestMetrics
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.*

internal class OriginValidationPolicy(private val cache: OriginValidationCache) {
    fun evaluate(call: ApplicationCall, surface: RequestSurface, descriptor: EndpointDescriptor?): SecurityPolicyDecision {
        val mutating = descriptor?.mutation == MutationKind.WRITE ||
            (descriptor == null && call.request.httpMethod in setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete))
        val origin = call.request.headers[HttpHeaders.Origin] ?: return SecurityPolicyDecision.Allowed
        if (!mutating || surface != RequestSurface.API) return SecurityPolicyDecision.Allowed
        val accepted = cache.getOrPut("$origin|${call.request.local.serverHost}:${call.request.local.serverPort}") {
            OriginPolicy(call.request.local.serverHost, call.request.local.serverPort).accepts(origin)
        }
        if (!accepted) logger.info {
            auditLog(AuditEvent(AuditEventName.ORIGIN_REJECTED, listOf(logField(StructuredLogKeys.REASON, origin))))
        }
        return if (accepted) SecurityPolicyDecision.Allowed else
            SecurityPolicyDecision.Rejected(SecurityRejectionReason.ORIGIN)
    }

    fun rejects(call: ApplicationCall, surface: RequestSurface, descriptor: EndpointDescriptor?): Boolean {
        return evaluate(call, surface, descriptor) is SecurityPolicyDecision.Rejected
    }
}

internal class RateLimitPolicyEnforcer(
    private val ipLimiter: RequestRateLimiter,
    private val accountLimiter: RequestRateLimiter
) {
    fun enforce(
        call: ApplicationCall,
        surface: RequestSurface,
        descriptor: EndpointDescriptor?,
        endpoint: RequestEndpoint,
        sessions: SessionRegistry,
        clock: ServerClock,
        metrics: RequestMetrics
    ) {
        if (surface != RequestSurface.API) return
        val scope = descriptor?.rateLimit ?: RateLimitScope.IP_AND_ACCOUNT
        val ip = if (scope == RateLimitScope.NONE) null else
            ipLimiter.tryAcquireDetailed(call.request.local.remoteHost, endpoint = endpoint)
        val account = (browserSession(call, sessions) as? BrowserSession.Found)?.session?.accountKey?.value
            ?.takeIf { scope == RateLimitScope.IP_AND_ACCOUNT }
            ?.let { accountLimiter.tryAcquireDetailed(it, endpoint = endpoint) }
        val decisions = listOfNotNull(ip, account)
        RateLimitHeaders.append(call, ip, account, clock.epochMillis())
        if (decisions.any { !it.allowed }) {
            metrics.recordRejected()
            logger.info {
                auditLog(AuditEvent(AuditEventName.RATE_LIMIT_REJECTED, listOf(logField(StructuredLogKeys.REASON, endpoint.value))))
            }
            call.response.headers.append(
                HttpHeaders.RetryAfter,
                RateLimitHeaders.retryAfterSeconds(decisions, clock.epochMillis())
            )
            throw ApiRateLimitedException()
        }
    }
}
