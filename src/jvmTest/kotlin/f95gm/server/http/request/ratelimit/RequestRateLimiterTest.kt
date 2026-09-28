package f95gm.server.http.request.ratelimit

import f95gm.server.http.request.RequestEndpoint
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

class RequestRateLimiterTest {
    private fun at(millis: Long) = RequestRateLimiter.RateLimitNow(
        f95gm.server.kernel.MonotonicMillis(millis),
        f95gm.server.model.EpochMillis(millis)
    )
    @Test
    fun rejectsInvalidConfiguration() {
        assertFailsWith<IllegalArgumentException> { RequestRateLimiter(0, 1_000) }
        assertFailsWith<IllegalArgumentException> { RequestRateLimiter(1, 0) }
    }

    @Test
    fun limitsRequestsWithinAWindowAndResetsAfterIt() {
        val limiter = RequestRateLimiter(limit = 2, windowMillis = 1_000)
        assertTrue(limiter.tryAcquire("client", at(0)))
        assertTrue(limiter.tryAcquire("client", at(1)))
        assertFalse(limiter.tryAcquire("client", at(2)))
        assertTrue(limiter.tryAcquire("client", at(1_000)))
    }

    @Test
    fun reportsRemainingBudgetAndResetTimeAtTheBoundary() {
        val limiter = RequestRateLimiter(limit = 2, windowMillis = 1_000)
        assertEquals(1, limiter.tryAcquireDetailed("client", at(100)).remaining)
        assertEquals(0, limiter.tryAcquireDetailed("client", at(200)).remaining)
        val rejected = limiter.tryAcquireDetailed("client", at(300))
        assertEquals(0, rejected.remaining)
        assertEquals(1_100L, rejected.resetAtMillis)
        assertFalse(rejected.allowed)
    }

    @Test
    fun keepsBudgetsIndependentPerEndpoint() {
        val limiter = RequestRateLimiter(limit = 1, windowMillis = 1_000)

        assertTrue(limiter.tryAcquire("client", at(0), RequestEndpoint("catalog")))
        assertTrue(limiter.tryAcquire("client", at(0), RequestEndpoint("marks")))
        assertFalse(limiter.tryAcquire("client", at(1), RequestEndpoint("catalog")))
    }

    @Test
    fun evictsOldestBucketWhenTheConfiguredCapacityIsExceeded() {
        val limiter = RequestRateLimiter(limit = 1, windowMillis = 1_000, maxEntries = 2)

        assertTrue(limiter.tryAcquire("first", at(0)))
        assertTrue(limiter.tryAcquire("second", at(10)))
        assertTrue(limiter.tryAcquire("third", at(20)))
        assertTrue(limiter.tryAcquire("first", at(21)))
    }

    @Test
    fun concurrentAcquisitionNeverExceedsTheConfiguredLimit() {
        val limiter = RequestRateLimiter(limit = 50, windowMillis = 60_000)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val results = (1..200).map { executor.submit<Boolean> { limiter.tryAcquire("shared", at(0)) } }
            assertEquals(50, results.count { it.get(5, TimeUnit.SECONDS) })
        } finally {
            executor.shutdownNow()
        }
    }
}
