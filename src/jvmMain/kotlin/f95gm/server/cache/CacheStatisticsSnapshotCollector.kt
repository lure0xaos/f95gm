package f95gm.server.cache

import f95gm.server.kernel.MonotonicMillis

/** Collects storage and telemetry diagnostics without exposing storage locking to the cache facade. */
internal class CacheStatisticsSnapshotCollector<K>(
    private val entries: CacheEntryCoordinator<K, *>,
    private val clock: () -> MonotonicMillis,
    private val namespaceTelemetry: CacheNamespaceTelemetry,
    private val telemetry: CacheTelemetry,
    private val trackedNamespace: (K) -> CacheNamespace
) {
    fun snapshot(): CacheStatisticsSnapshot {
        val entryCounts = entries.snapshotKeys(clock()).groupingBy(trackedNamespace).eachCount()
        return CacheStatisticsSnapshot(
            namespaces = namespaceTelemetry.snapshotValue(entryCounts),
            totals = telemetry.snapshotValue()
        )
    }
}
