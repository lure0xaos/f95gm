package f95gm.server.http.request

import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

class RequestMetricsBenchmarkTest {
    @Test
    fun repeatedSnapshotsStayWithinTheAllocationRegressionBudget() {
        val metrics = RequestMetrics()
        repeat(10_000) { metrics.record(RequestDuration(it.toLong()), RequestEndpoint("/api/metrics")) }
        val elapsed = measureTimeMillis { repeat(20_000) { metrics.snapshot() } }
        assertTrue(elapsed < 5_000, "metrics snapshot regression: ${elapsed}ms")
    }
}
