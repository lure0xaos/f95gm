package f95gm.server.http.request

import kotlin.test.Test
import kotlin.test.assertEquals

class RequestMetricsTest {
    @Test
    fun recordsOperationalCountersAlongsideRequests() {
        val metrics = RequestMetrics()
        metrics.record(12)
        metrics.rejected.incrementAndGet()
        metrics.recordRetry()
        metrics.recordEviction()
        metrics.recordCircuitOpening()

        assertEquals(1, metrics.requests.get())
        assertEquals(1, metrics.rejected.get())
        assertEquals(12, metrics.totalDurationMillis.get())
        assertEquals(1, metrics.retries.get())
        assertEquals(1, metrics.evictions.get())
        assertEquals(1, metrics.circuitOpenings.get())
    }

    @Test
    fun snapshotCapturesAllCountersAsOneValueObject() {
        val metrics = RequestMetrics()
        metrics.record(5)
        metrics.record(7)
        metrics.rejected.addAndGet(2)
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
}
