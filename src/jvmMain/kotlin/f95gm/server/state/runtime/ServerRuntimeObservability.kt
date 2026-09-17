package f95gm.server.state.runtime

import f95gm.server.application.RegistrySnapshotCoordinator
import f95gm.server.application.RuntimeRegistryDiagnostics

import f95gm.server.http.request.RequestMetrics
import f95gm.server.cache.CacheStatisticsProvider
import f95gm.server.http.request.RequestMetricsSnapshot

/** Keeps runtime metrics wiring separate from service construction. */
internal class ServerRuntimeObservability(
    val requestMetrics: RequestMetrics,
    private val cacheStatsProvider: CacheStatisticsProvider
) {
    fun snapshot(registries: RuntimeRegistryDiagnostics = RuntimeRegistryDiagnostics()): ServerObservabilitySnapshot =
        ServerObservabilitySnapshot(requestMetrics.snapshot(cacheStatsProvider), registries)
}

/** Immutable diagnostic surface shared by health, support tooling, and tests. */
internal data class ServerObservabilitySnapshot(
    val requestMetrics: RequestMetricsSnapshot,
    val registries: RuntimeRegistryDiagnostics = RuntimeRegistryDiagnostics()
)
