package f95gm.server.cache

import f95gm.server.kernel.MetricNames
import f95gm.server.kernel.BoundedCardinality
import f95gm.server.kernel.MetricCounter
import f95gm.server.kernel.ConsistentSnapshotSequence
import f95gm.server.kernel.ResourceLock
import java.util.HashMap

/** Bounds namespace cardinality and owns per-namespace counters independently of cache storage. */
internal class CacheNamespaceTelemetry(
    maxNamespaces: Int,
    private val updateSequence: ConsistentSnapshotSequence
) {
    private val counters = HashMap<CacheNamespace, NamespaceCounters>()
    private val counterLock = ResourceLock()
    private val cardinality = BoundedCardinality<CacheNamespace>(maxNamespaces - 1)

    fun tracked(namespace: CacheNamespace): CacheNamespace =
        if (namespace.value == MetricNames.OTHER_NAMESPACE || cardinality.admit(namespace)) namespace
        else CacheNamespace(MetricNames.OTHER_NAMESPACE)

    fun hit(namespace: CacheNamespace) = updateSequence.withUpdate { counters(namespace).hits.increment() }
    fun miss(namespace: CacheNamespace) = updateSequence.withUpdate { counters(namespace).misses.increment() }
    fun eviction(namespace: CacheNamespace, expired: Boolean) {
        updateSequence.withUpdate {
            val target = counters(namespace)
            if (expired) target.expiredEvictions.increment() else target.evictions.increment()
        }
    }

    fun snapshot(entries: Map<CacheNamespace, Int>): Map<CacheNamespace, CacheNamespaceStats> =
        updateSequence.read(3) { snapshotValue(entries) }.let { result ->
            result.value.mapValues { (_, stats) -> stats.copy(countersConsistent = result.consistent) }
        }

    fun snapshotValue(entries: Map<CacheNamespace, Int>): Map<CacheNamespace, CacheNamespaceStats> =
        snapshotOnce(entries)

    private fun snapshotOnce(entries: Map<CacheNamespace, Int>): Map<CacheNamespace, CacheNamespaceStats> =
        counterLock.withLock {
            (entries.keys + counters.keys).associate { namespace ->
                val stats = counters[namespace] ?: NamespaceCounters()
                namespace to CacheNamespaceStats(
                    entries = entries[namespace]?.toLong() ?: 0L,
                    hits = stats.hits.get(),
                    misses = stats.misses.get(),
                    evictions = stats.evictions.get(),
                    expiredEvictions = stats.expiredEvictions.get()
                )
            }.toList().sortedBy { it.first.value }.toMap()
        }

    private fun counters(namespace: CacheNamespace): NamespaceCounters = counterLock.withLock {
        counters.getOrPut(namespace) { NamespaceCounters() }
    }

    private class NamespaceCounters {
        val hits = MetricCounter()
        val misses = MetricCounter()
        val evictions = MetricCounter()
        val expiredEvictions = MetricCounter()
    }
}
