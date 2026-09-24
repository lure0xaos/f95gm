package f95gm.server.app

import f95gm.domain.api.F95Api
import f95gm.server.config.Bytes
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.logging.RequestCompletedEvent
import f95gm.server.config.logging.logger
import f95gm.server.config.logging.structuredLog
import f95gm.server.config.security.ServerSecurityConfig
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.request.*
import f95gm.server.http.request.origin.OriginPolicy
import f95gm.server.http.request.origin.OriginValidationCache
import f95gm.server.http.request.ratelimit.RateLimitHeaders
import f95gm.server.http.request.ratelimit.RateLimitPolicy
import f95gm.server.http.request.ratelimit.RequestRateLimiter
import f95gm.server.http.response.ApiCsrfException
import f95gm.server.http.response.ApiRateLimitedException
import f95gm.server.http.response.ApiRequestTooLargeException
import f95gm.server.routes.session.attachAuthenticatedSession
import f95gm.server.routes.session.browserSession
import f95gm.server.routes.session.hasAuthorizedAccount
import f95gm.server.routes.session.respondAuthenticationRequired
import f95gm.server.state.runtime.ServerContext
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.nanoseconds

internal data class RequestMonitoringConfiguration(
    val maxRequestBodyBytes: Bytes,
    val rateLimitPolicy: RateLimitPolicy,
    val maxRateLimitBuckets: RateLimitBucketLimit,
    val requestIdGenerator: RequestIdGenerator = SecureRequestIdentifiers.requestIds
)

internal fun Application.installRequestMonitoring(
    shutdownController: () -> ServerShutdownController,
    routeContext: ServerContext,
    configuration: RequestMonitoringConfiguration
) = installRequestMonitoringWithPolicy(
    shutdownController, routeContext, configuration
)

private fun Application.installRequestMonitoringWithPolicy(
    shutdownController: () -> ServerShutdownController,
    routeContext: ServerContext,
    configuration: RequestMonitoringConfiguration
) {
    val ipRateLimiter = RequestRateLimiter(
        configuration.rateLimitPolicy.ipRequestsPerWindow.value,
        configuration.rateLimitPolicy.windowMillis.value,
        maxEntries = configuration.maxRateLimitBuckets.value
    )
    val accountRateLimiter = RequestRateLimiter(
        configuration.rateLimitPolicy.accountRequestsPerWindow.value,
        configuration.rateLimitPolicy.windowMillis.value,
        maxEntries = configuration.maxRateLimitBuckets.value
    )
    val originCache = OriginValidationCache(clock = routeContext.clock)
    val metrics = routeContext.requestMetrics
    val endpointResolver = ApiEndpointResolver()
    monitor.subscribe(ApplicationStopped) {
        ipRateLimiter.close()
        accountRateLimiter.close()
        originCache.close()
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        call.appendSecurityHeaders()
        val requestId = call.requestIdHeader()
            ?.let { runCatching { RequestId(it) }.getOrNull() }
            ?: configuration.requestIdGenerator.next()
        val requestContext = RequestContext(requestId, call.request.local.remoteHost)
        call.installRequestContext(requestContext)
        call.response.headers.append(ServerConstants.REQUEST_ID_HEADER, requestId.value)
        val started = routeContext.clock.monotonicNanos()
        val requestLease = routeContext.activeRequests.enter()
        var failure: Throwable? = null
        var metricEndpoint = "unmatched"
        try {
            if (shutdownController().isDraining() && !call.isPublicDuringDrain()) {
                call.respond(ServerConstants.STATUS_SERVICE_UNAVAILABLE)
                finish()
                return@intercept
            }

            val requestPath = call.request.requestPath().value
            val isApiRequest = endpointResolver.isApiPath(requestPath)
            val descriptor = endpointResolver.resolve(requestPath, call.request.httpMethod)
            metricEndpoint = descriptor?.path ?: metricEndpoint
            val endpoint = RequestEndpoint(descriptor?.path ?: requestPath)
            val mutating = descriptor?.mutation == F95Api.MutationKind.WRITE ||
                    (descriptor == null && call.request.httpMethod in MUTATING_METHODS)
            val origin = call.request.headers[HttpHeaders.Origin]
            if (mutating && isApiRequest && origin != null &&
                !originCache.getOrPut("$origin|${call.request.local.serverHost}:${call.request.local.serverPort}") {
                    OriginPolicy(call.request.local.serverHost, call.request.local.serverPort).accepts(origin)
                }
            ) {
                throw ApiCsrfException()
            }
            if (isApiRequest) {
                try {
                    call.validateApiContentLength(configuration.maxRequestBodyBytes.value)
                } catch (error: ApiRequestTooLargeException) {
                    metrics.rejected.incrementAndGet()
                    throw error
                }
            }
            if (isApiRequest) {
                val rateLimit = descriptor?.rateLimit ?: F95Api.RateLimitScope.IP_AND_ACCOUNT
                val ipDecision = if (rateLimit == F95Api.RateLimitScope.NONE) null else
                    ipRateLimiter.tryAcquireDetailed(call.request.local.remoteHost, endpoint = endpoint)
                val accountKey = browserSession(call, routeContext.sessions)?.second?.accountKey?.value
                val accountDecision =
                    if (rateLimit != F95Api.RateLimitScope.IP_AND_ACCOUNT || accountKey == null) null else
                        accountRateLimiter.tryAcquireDetailed(accountKey, endpoint = endpoint)
                val decisions = listOfNotNull(ipDecision, accountDecision)
                RateLimitHeaders.append(call, ipDecision, accountDecision, routeContext.clock.nowMillis())
                if (decisions.any { !it.allowed }) {
                    metrics.rejected.incrementAndGet()
                    call.response.headers.append(
                        HttpHeaders.RetryAfter,
                        RateLimitHeaders.retryAfterSeconds(decisions, routeContext.clock.nowMillis())
                    )
                    throw ApiRateLimitedException()
                }
            }
            if (descriptor?.auth == F95Api.AuthRequirement.SESSION) {
                val session = browserSession(call, routeContext.sessions)?.second
                if (session == null || !session.hasAuthorizedAccount()) {
                    call.respondAuthenticationRequired()
                    finish()
                    return@intercept
                }
                call.attachAuthenticatedSession(session)
            }

            withContext(requestContext.asCoroutineContext()) {
                proceed()
            }
        } catch (error: Exception) {
            failure = error
            throw error
        } finally {
            val duration = (routeContext.clock.monotonicNanos() - started).nanoseconds.inWholeMilliseconds
            metrics.record(duration, metricEndpoint)
            logger.info {
                structuredLog(
                    RequestCompletedEvent(
                        requestId = requestId.value,
                        method = call.request.httpMethod.value,
                        path = call.request.requestPath().value,
                        status = call.response.status()?.value ?: "-",
                        durationMs = duration,
                        outcome = if (failure == null) "success" else "error",
                        errorType = failure?.javaClass?.simpleName
                    )
                )
            }
            requestLease.close()
        }
    }
}

private val MUTATING_METHODS = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete)

private fun ApplicationCall.appendSecurityHeaders() {
    response.headers.append("X-Content-Type-Options", ServerSecurityConfig.CONTENT_TYPE_OPTIONS)
    response.headers.append("X-Frame-Options", ServerSecurityConfig.FRAME_OPTIONS)
    response.headers.append("Permissions-Policy", ServerSecurityConfig.PERMISSIONS_POLICY)
    response.headers.append("Referrer-Policy", ServerSecurityConfig.REFERRER_POLICY)
    val nonce = ServerSecurityConfig.nonce()
    attributes.put(ServerSecurityConfig.CSP_NONCE_ATTRIBUTE, nonce)
    response.headers.append("Content-Security-Policy", ServerSecurityConfig.contentSecurityPolicy(nonce))
}
