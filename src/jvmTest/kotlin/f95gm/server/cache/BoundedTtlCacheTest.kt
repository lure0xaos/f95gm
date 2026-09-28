package f95gm.server.cache

import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

class BoundedTtlCacheTest {
    @Test
    fun exposesShutdownRejectionsInStatistics() = runBlocking {
        val cache = BoundedTtlCache<String, Int>(2, 60.seconds)
        cache.close()

        assertFailsWith<IllegalStateException> { cache.getOrPut("sync") { 1 } }
        assertFailsWith<IllegalStateException> { cache.getOrPutSuspending("async") { 1 } }
        assertEquals(2, cache.stats().shutdownRejections)
    }

    @Test
    fun compositeProviderPrefixesNamespacesWithoutDroppingStatistics() {
        val provider = CompositeCacheStatisticsProvider(listOf(
            CacheStatisticsSource(CacheNamespace("live"), CacheStatisticsProvider {
                CacheStatisticsSnapshot(
                    namespaces = mapOf(CacheNamespace("marks") to CacheNamespaceStats(2, 3, 1, 0, 0)),
                    totals = CacheStats()
                )
            }),
            CacheStatisticsSource(CacheNamespace("static"), CacheStatisticsProvider {
                CacheStatisticsSnapshot(
                    namespaces = mapOf(CacheNamespace("classpath") to CacheNamespaceStats(1, 4, 0, 1, 0)),
                    totals = CacheStats()
                )
            })
        ))

        assertEquals(
            setOf(CacheNamespace("live:marks"), CacheNamespace("static:classpath")),
            provider.snapshot().namespaces.keys
        )
        assertEquals(3L, provider.snapshot().namespaces.getValue(CacheNamespace("live:marks")).hits)
    }

    @Test
    fun compositeProviderCanAggregateExistingWireNamespacesWithoutRequalifyingThem() {
        val provider = CompositeCacheStatisticsProvider(
            listOf(
                CacheStatisticsSource(CacheNamespaces.LIVE_MARK, CacheStatisticsProvider {
                    CacheStatisticsSnapshot(
                        namespaces = mapOf(CacheNamespaces.LIVE_MARK to CacheNamespaceStats(1, 2, 0, 0, 0)),
                        totals = CacheStats()
                    )
                }),
                CacheStatisticsSource(CacheNamespaces.CLASSPATH, CacheStatisticsProvider {
                    CacheStatisticsSnapshot(
                        namespaces = mapOf(CacheNamespaces.CLASSPATH to CacheNamespaceStats(3, 4, 0, 0, 0)),
                        totals = CacheStats()
                    )
                })
            ),
            qualifyNamespaces = false
        )

        assertEquals(setOf(CacheNamespaces.LIVE_MARK, CacheNamespaces.CLASSPATH), provider.snapshot().namespaces.keys)
    }

    @Test
    fun invalidationPreventsAnOlderLoadFromRepopulatingTheCache() {
        val cache = BoundedTtlCache<String, Int>(2, 60.seconds)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val first = executor.submit<Int> {
                cache.getOrPut("key") {
                    started.countDown()
                    release.await()
                    1
                }
            }
            check(started.await(5, TimeUnit.SECONDS))
            cache.invalidate("key")
            release.countDown()
            assertEquals(1, first.get(5, TimeUnit.SECONDS))
            assertEquals(2, cache.getOrPut("key") { 2 })
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun coalescesConcurrentLoadersThroughTheSuspendingApi() {
        val cache = BoundedTtlCache<String, Int>(4, 60.seconds)
        var loads = 0
        kotlinx.coroutines.test.runTest {
            val first = cache.getOrPutSuspending("key") { ++loads }
            val second = cache.getOrPutSuspending("key") { ++loads }
            assertEquals(1, first)
            assertEquals(1, second)
            assertEquals(1, loads)
        }
    }

    @Test
    fun recordsHitsMissesAndEvictions() {
        val cache = BoundedTtlCache<String, Int>(1, 60.seconds)
        cache.getOrPut("a") { 1 }
        cache.getOrPut("a") { 2 }
        cache.getOrPut("b") { 3 }
        assertEquals(CacheStats(hits = 1, misses = 2, evictions = 1), cache.stats())
        assertEquals(3, cache.stats().lookups)
        assertEquals(1.0 / 3.0, cache.stats().hitRate)
        assertEquals(2.0 / 3.0, cache.stats().missRate)
    }

    @Test
    fun reportsEvictionsToOperationalMetrics() {
        var evictions = 0
        val cache = BoundedTtlCache<String, Int>(1, 60.seconds, onEvent = { evictions++ })
        cache.getOrPut("a") { 1 }
        cache.getOrPut("b") { 2 }
        assertEquals(1, evictions)
    }

    @Test
    fun expiredEntriesAreReloadedWithoutBeingCountedAsCapacityEvictions() {
        val cache = BoundedTtlCache<String, Int>(2, ttl = 10.milliseconds)
        cache.getOrPutAt("expired", now = 0) { 1 }
        cache.getOrPutAt("fresh", now = 5) { 2 }

        assertEquals(3, cache.getOrPutAt("expired", now = 10) { 3 })
        assertEquals(2, cache.getOrPutAt("fresh", now = 10) { 4 })
        assertEquals(CacheStats(hits = 1, misses = 3, evictions = 0, expiredEvictions = 1), cache.stats())
    }

    @Test
    fun tracksStatisticsPerNamespace() {
        val cache = BoundedTtlCache<String, Int>(4, 1.seconds, namespaceOf = { CacheNamespace(it.substringBefore(':')) })
        cache.getOrPut("a:1") { 1 }
        cache.getOrPut("a:1") { 2 }
        cache.getOrPut("b:1") { 3 }

        val stats = cache.snapshot().namespaces
        assertEquals(1, stats[CacheNamespace("a")]?.hits)
        assertEquals(1, stats[CacheNamespace("a")]?.misses)
        assertEquals(1, stats[CacheNamespace("a")]?.entries)
        assertEquals(1, stats[CacheNamespace("b")]?.misses)
    }

    @Test
    fun coalescesConcurrentLoadersThroughTheSynchronousApi() {
        val cache = BoundedTtlCache<String, Int>(4, 60.seconds)
        val loads = AtomicInteger()
        val loaderEntered = CountDownLatch(1)
        val releaseLoader = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val futures = (1..32).map {
                executor.submit<Int> {
                    cache.getOrPut("key") {
                        loads.incrementAndGet()
                        loaderEntered.countDown()
                        releaseLoader.await(5, TimeUnit.SECONDS)
                        42
                    }
                }
            }
            assertTrue(loaderEntered.await(5, TimeUnit.SECONDS))
            releaseLoader.countDown()
            futures.forEach { assertEquals(42, it.get(5, TimeUnit.SECONDS)) }
            assertEquals(1, loads.get())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun propagatesLoaderFailureAndAllowsALaterRetry() {
        val cache = BoundedTtlCache<String, Int>(2, 60.seconds)
        assertFailsWith<IllegalStateException> {
            cache.getOrPut("key") { error("first failure") }
        }
        assertEquals(7, cache.getOrPut("key") { 7 })
    }

    @Test
    fun clearRemovesCachedValuesAndResetsLoadCoordination() {
        val cache = BoundedTtlCache<String, Int>(2, 60.seconds)
        var loads = 0
        assertEquals(1, cache.getOrPut("key") { ++loads })
        cache.clear()
        assertEquals(2, cache.getOrPut("key") { ++loads })
        assertEquals(2, loads)
    }

    @Test
    fun boundedCacheInvariantHoldsAcrossManyDistinctKeys() {
        val cache = BoundedTtlCache<String, Int>(4, 60.seconds)
        repeat(500) { index -> cache.getOrPut("key-$index") { index } }
        assertTrue(cache.snapshot().namespaces.values.sumOf { it.entries } <= 4)
    }

    @Test
    fun suspendingLoaderFailureDoesNotPoisonTheKey() = kotlinx.coroutines.test.runTest {
        val cache = BoundedTtlCache<String, Int>(2, 60.seconds)
        assertFailsWith<IllegalStateException> {
            cache.getOrPutSuspending("key") { error("failed") }
        }
        assertEquals(9, cache.getOrPutSuspending("key") { 9 })
    }
}
