package f95gm.server.http.request

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class RequestMetrics {
    val requests = AtomicLong()
    val rejected = AtomicLong()
    val totalDurationMillis = AtomicLong()
    val retries = AtomicLong()
    val evictions = AtomicLong()
    val circuitOpenings = AtomicLong()
    private val endpointRequests = ConcurrentHashMap<String, AtomicLong>()

    fun snapshot(): RequestMetricsSnapshot = RequestMetricsSnapshot(
        requests.get(),
        rejected.get(),
        totalDurationMillis.get(),
        retries.get(),
        evictions.get(),
        circuitOpenings.get(),
        endpointRequests.entries.sortedBy { it.key }.associate { it.key to it.value.get() }
    )

    fun record(durationMillis: Long, endpoint: String? = null) {
        requests.incrementAndGet()
        totalDurationMillis.addAndGet(durationMillis)
        endpoint?.let { endpointRequests.computeIfAbsent(it) { AtomicLong() }.incrementAndGet() }
    }

    fun recordRetry() = retries.incrementAndGet()
    fun recordEviction() = evictions.incrementAndGet()
    fun recordCircuitOpening() = circuitOpenings.incrementAndGet()
}
