package f95gm.server.http.request

import f95gm.domain.api.CacheNamespaceMetricsContract
import f95gm.server.cache.CacheNamespaceStats
import f95gm.server.cache.CacheNamespace

/** Converts internal cache statistics into the request metrics contract at one boundary. */
internal object MetricSnapshotMappers {
    fun cacheNamespaces(
        statistics: Map<CacheNamespace, CacheNamespaceStats>
    ): Map<MetricNamespace, CacheNamespaceMetricsContract> = statistics
        .mapKeys { (namespace, _) -> MetricNamespace(namespace.value) }
        .mapValues { (_, stats) ->
            CacheNamespaceMetricsContract(
                stats.entries,
                stats.hits,
                stats.misses,
                stats.evictions,
                stats.expiredEvictions,
                stats.countersConsistent
            )
        }
}
