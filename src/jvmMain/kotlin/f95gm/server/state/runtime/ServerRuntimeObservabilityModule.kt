package f95gm.server.state.runtime

import f95gm.server.cache.CacheStatisticsProvider
import f95gm.server.cache.CacheStatisticsSnapshot
import f95gm.server.cache.CacheStats
import f95gm.server.config.ServerConfig
import f95gm.server.http.request.RequestMetrics

/** Keeps metrics construction and late cache wiring out of the runtime composition root. */
internal class ServerRuntimeObservabilityModule private constructor(val requestMetrics: RequestMetrics) {

    companion object {
        fun create(config: ServerConfig): ServerRuntimeObservabilityModule {
            return ServerRuntimeObservabilityModule(
                RequestMetrics(
                    config.limits.maxMetricEndpoints,
                    config.limits.maxUpstreamMetricEndpoints,
                    CacheStatisticsProvider {
                        CacheStatisticsSnapshot(namespaces = emptyMap(), totals = CacheStats())
                    }
                )
            )
        }
    }
}
