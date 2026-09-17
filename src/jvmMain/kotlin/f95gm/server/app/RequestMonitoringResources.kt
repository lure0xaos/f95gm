package f95gm.server.app

import f95gm.server.http.request.origin.OriginValidationCache
import f95gm.server.http.request.ratelimit.RequestRateLimiter
import f95gm.server.kernel.EntryCapacity
import f95gm.server.http.request.ratelimit.RateLimitWindow
import f95gm.server.http.request.ratelimit.RequestRateLimit
import f95gm.server.kernel.ServerClock
import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource
import f95gm.server.config.ServerConfigurationState
import f95gm.server.config.ConfigurationReloadResult

internal class RequestMonitoringResources(
    configuration: RequestMonitoringConfiguration,
    clock: ServerClock,
    configurationState: ServerConfigurationState? = null
) : LifecycleResource {
    private val closeGuard = CloseGuard()
    val ipRateLimiter = RequestRateLimiter(
        limit = configuration.rateLimitPolicy.ipRequestsPerWindow.value,
        windowMillis = configuration.rateLimitPolicy.windowMillis.value,
        clock = clock,
        maxEntries = configuration.maxRateLimitBuckets.value
    )
    val accountRateLimiter = RequestRateLimiter(
        limit = configuration.rateLimitPolicy.accountRequestsPerWindow.value,
        windowMillis = configuration.rateLimitPolicy.windowMillis.value,
        clock = clock,
        maxEntries = configuration.maxRateLimitBuckets.value
    )
    val originCache = OriginValidationCache(clock = clock)
    val originPolicy = OriginValidationPolicy(originCache)
    val rateLimitPolicy = RateLimitPolicyEnforcer(ipRateLimiter, accountRateLimiter)
    private val configurationSubscription = configurationState?.subscribe(::applyReload)

    private fun applyReload(result: ConfigurationReloadResult) {
        val traffic = result.current.value.traffic
        val window = RateLimitWindow.fromMillis(traffic.rateLimitWindowMillis.value)
        ipRateLimiter.updatePolicy(RequestRateLimit(traffic.ipRequestsPerMinute.value), window)
        accountRateLimiter.updatePolicy(RequestRateLimit(traffic.accountRequestsPerMinute.value), window)
    }

    override val isClosed: Boolean get() = closeGuard.isClosed
    override fun close() = closeGuard.closeOnce {
        ipRateLimiter.close()
        accountRateLimiter.close()
        originCache.close()
        configurationSubscription?.close()
    }
}
