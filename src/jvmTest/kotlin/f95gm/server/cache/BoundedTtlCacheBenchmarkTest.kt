package f95gm.server.cache

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** Coarse regression benchmark; it intentionally avoids a flaky hard performance budget. */
class BoundedTtlCacheBenchmarkTest {
    @Test
    fun concurrentLookupsStayWithinTheContentionRegressionBudget() {
        val cache = BoundedTtlCache<String, Int>(256, 60.seconds)
        val executor = Executors.newFixedThreadPool(8)
        val elapsed = measureTimeMillis {
            repeat(20_000) { index ->
                executor.submit { cache.getOrPut("key-${index % 256}") { index } }
            }
            executor.shutdown()
            check(executor.awaitTermination(10, TimeUnit.SECONDS))
        }
        executor.shutdownNow()
        assertTrue(elapsed < 10_000, "cache contention regression: ${elapsed}ms")
    }
}
