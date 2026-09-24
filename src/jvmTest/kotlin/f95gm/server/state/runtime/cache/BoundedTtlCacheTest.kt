package f95gm.server.state.runtime.cache

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BoundedTtlCacheTest {
    @Test
    fun coalescesConcurrentLoadersThroughTheSuspendingApi() {
        val cache = BoundedTtlCache<String, Int>(4, 60_000L)
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
        val cache = BoundedTtlCache<String, Int>(1, 60_000L)
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
        val cache = BoundedTtlCache<String, Int>(1, 60_000L, onEviction = { evictions++ })
        cache.getOrPut("a") { 1 }
        cache.getOrPut("b") { 2 }
        assertEquals(1, evictions)
    }

    @Test
    fun expiredEntriesAreReloadedWithoutBeingCountedAsCapacityEvictions() {
        val cache = BoundedTtlCache<String, Int>(2, ttlMillis = 10)
        cache.getOrPutAt("expired", now = 0) { 1 }
        cache.getOrPutAt("fresh", now = 5) { 2 }

        assertEquals(3, cache.getOrPutAt("expired", now = 10) { 3 })
        assertEquals(2, cache.getOrPutAt("fresh", now = 10) { 4 })
        assertEquals(CacheStats(hits = 1, misses = 3, evictions = 0), cache.stats())
    }

    @Test
    fun coalescesConcurrentLoadersThroughTheSynchronousApi() {
        val cache = BoundedTtlCache<String, Int>(4, 60_000L)
        val loads = AtomicInteger()
        val executor = Executors.newFixedThreadPool(8)
        try {
            val futures = (1..32).map {
                executor.submit<Int> {
                    cache.getOrPut("key") {
                        loads.incrementAndGet()
                        Thread.sleep(5)
                        42
                    }
                }
            }
            futures.forEach { assertEquals(42, it.get(5, TimeUnit.SECONDS)) }
            assertEquals(1, loads.get())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun propagatesLoaderFailureAndAllowsALaterRetry() {
        val cache = BoundedTtlCache<String, Int>(2, 60_000L)
        assertFailsWith<IllegalStateException> {
            cache.getOrPut("key") { error("first failure") }
        }
        assertEquals(7, cache.getOrPut("key") { 7 })
    }

    @Test
    fun clearRemovesCachedValuesAndResetsLoadCoordination() {
        val cache = BoundedTtlCache<String, Int>(2, 60_000L)
        var loads = 0
        assertEquals(1, cache.getOrPut("key") { ++loads })
        cache.clear()
        assertEquals(2, cache.getOrPut("key") { ++loads })
        assertEquals(2, loads)
    }

    @Test
    fun suspendingLoaderFailureDoesNotPoisonTheKey() = kotlinx.coroutines.test.runTest {
        val cache = BoundedTtlCache<String, Int>(2, 60_000L)
        assertFailsWith<IllegalStateException> {
            cache.getOrPutSuspending("key") { error("failed") }
        }
        assertEquals(9, cache.getOrPutSuspending("key") { 9 })
    }
}
