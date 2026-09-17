package f95gm.server.http.request

import f95gm.server.cache.CacheNamespaceStats
import f95gm.server.cache.CacheNamespace
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue

class RequestMetricsConcurrencyTest {
    @Test
    fun concurrentSnapshotsRemainNonNegativeAndStableByNamespace() {
        val metrics = RequestMetrics(cacheStatsProvider = f95gm.server.cache.CacheStatisticsProvider {
            f95gm.server.cache.CacheStatisticsSnapshot(
                mapOf(CacheNamespace("live") to CacheNamespaceStats(1, 2, 3, 4, 5)),
                f95gm.server.cache.CacheStats()
            )
        })
        val executor = Executors.newFixedThreadPool(8)
        try {
            repeat(2_000) { index ->
                executor.submit {
                    metrics.record(RequestDuration(index.toLong()), RequestEndpoint("/metrics"))
                    metrics.snapshot()
                }
            }
            executor.shutdown()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
            val snapshot = metrics.snapshot()
            assertTrue(snapshot.requests >= 0)
            assertTrue(snapshot.totalDurationMillis >= 0)
            assertTrue(snapshot.cacheNamespaces.keys == setOf(MetricNamespace("live")))
        } finally {
            executor.shutdownNow()
        }
    }
}
