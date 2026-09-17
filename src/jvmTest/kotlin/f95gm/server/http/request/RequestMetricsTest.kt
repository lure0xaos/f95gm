package f95gm.server.http.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.util.concurrent.atomic.AtomicInteger
import f95gm.server.cache.CacheNamespace

class RequestMetricsTest {
    @Test
    fun recordsOperationalCountersAlongsideRequests() {
        val metrics = RequestMetrics()
        metrics.record(RequestDuration(12))
        metrics.recordRejected()
        metrics.recordRetry()
        metrics.recordEviction()
        metrics.recordCircuitOpening()

        assertEquals(
            RequestMetricsSnapshot(
                requests = 1,
                rejected = 1,
                totalDurationMillis = 12,
                retries = 1,
                evictions = 1,
                circuitOpenings = 1
            ),
            metrics.snapshot()
        )
    }

    @Test
    fun snapshotCapturesAllCountersAsOneValueObject() {
        val metrics = RequestMetrics()
        metrics.record(RequestDuration(5))
        metrics.record(RequestDuration(7))
        metrics.recordRejected()
        metrics.recordRejected()
        metrics.recordRetry()

        assertEquals(
            RequestMetricsSnapshot(
                requests = 2,
                rejected = 2,
                totalDurationMillis = 12,
                retries = 1,
                evictions = 0,
                circuitOpenings = 0
            ),
            metrics.snapshot()
        )
    }

    @Test
    fun snapshotIncludesOperationalCacheNamespaceMetrics() {
        val metrics = RequestMetrics(cacheStatsProvider = f95gm.server.cache.CacheStatisticsProvider {
            f95gm.server.cache.CacheStatisticsSnapshot(
                mapOf(CacheNamespace("live-mark") to f95gm.server.cache.CacheNamespaceStats(2, 3, 4, 5, 6)),
                f95gm.server.cache.CacheStats()
            )
        })

        val namespace = metrics.snapshot().cacheNamespaces.getValue(MetricNamespace("live-mark"))
        assertEquals(2, namespace.entries)
        assertEquals(3, namespace.hits)
        assertEquals(4, namespace.misses)
        assertEquals(5, namespace.evictions)
        assertEquals(6, namespace.expiredEvictions)
    }

    @Test
    fun snapshotReadsCacheProviderOnceForConsistentTotalsAndNamespaces() {
        val snapshots = AtomicInteger()
        val metrics = RequestMetrics(cacheStatsProvider = object : f95gm.server.cache.CacheStatisticsProvider {
            override fun snapshot() = f95gm.server.cache.CacheStatisticsSnapshot(
                namespaces = mapOf(CacheNamespace("live") to f95gm.server.cache.CacheNamespaceStats(1, 2, 3, 4, 5)),
                totals = f95gm.server.cache.CacheStats(hits = 2, misses = 3, evictions = 4, expiredEvictions = 5)
            ).also { snapshots.incrementAndGet() }
        })

        val snapshot = metrics.snapshot()

        assertEquals(2, snapshots.get()) // One initial read plus one consistency check.
        assertEquals(2, snapshot.cacheNamespaces.getValue(MetricNamespace("live")).hits)
        assertTrue(snapshot.countersConsistent)
    }
}
