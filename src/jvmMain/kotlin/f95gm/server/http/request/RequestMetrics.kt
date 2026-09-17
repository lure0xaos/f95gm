package f95gm.server.http.request

import f95gm.server.observability.UpstreamMetrics
import java.util.concurrent.ConcurrentHashMap
import f95gm.server.kernel.MetricNames
import f95gm.server.cache.CacheStatisticsProvider
import f95gm.server.cache.CacheStatisticsSnapshot
import f95gm.server.cache.CacheStats
import f95gm.server.kernel.BoundedCardinality
import f95gm.server.kernel.MetricCounter
import f95gm.server.kernel.ConsistentSnapshotSequence
import kotlin.time.Duration

@JvmInline
internal value class RequestDuration(val value: Long) {
    init { require(value >= 0L) }
}

internal class RequestMetrics(
    maxEndpointCardinality: Int = DEFAULT_MAX_ENDPOINTS,
    maxUpstreamEndpointCardinality: Int = UpstreamMetrics.DEFAULT_MAX_ENDPOINTS,
    private val cacheStatsProvider: CacheStatisticsProvider = CacheStatisticsProvider {
        CacheStatisticsSnapshot(namespaces = emptyMap(), totals = CacheStats())
    }
) {
    companion object {
        const val DEFAULT_MAX_ENDPOINTS = 256
        private const val SNAPSHOT_ATTEMPTS = 3
    }

    init { require(maxEndpointCardinality > 0) }
    private val upstream = UpstreamMetrics(maxUpstreamEndpointCardinality)
    private val requests = MetricCounter()
    private val rejected = MetricCounter()
    private val totalDurationMillis = MetricCounter()
    private val retries = MetricCounter()
    private val evictions = MetricCounter()
    private val circuitOpenings = MetricCounter()
    private val endpointRequests = ConcurrentHashMap<RequestEndpoint, MetricCounter>()
    private val endpointCardinality = BoundedCardinality<RequestEndpoint>(
        (maxEndpointCardinality - 1).coerceAtLeast(0)
    )
    private val updateSequence = ConsistentSnapshotSequence()
    internal fun upstreamMetrics(): UpstreamMetrics = upstream
    fun snapshot(snapshotCacheStatsProvider: CacheStatisticsProvider = cacheStatsProvider): RequestMetricsSnapshot {
        val result = updateSequence.read(SNAPSHOT_ATTEMPTS) {
            snapshotOnce(snapshotCacheStatsProvider)
        }
        return result.value.copy(countersConsistent = result.consistent && result.value.countersConsistent)
    }

    private fun snapshotOnce(snapshotCacheStatsProvider: CacheStatisticsProvider): RequestMetricsSnapshot {
        val before = updateSequence.current()
        val upstreamSnapshot = upstream.snapshot()
        val cacheSnapshot = snapshotCacheStatsProvider.snapshot()
        val snapshot = RequestMetricsSnapshot(
            requests = requests.get(),
            rejected = rejected.get(),
            totalDurationMillis = totalDurationMillis.get(),
            retries = retries.get(),
            evictions = evictions.get(),
            circuitOpenings = circuitOpenings.get(),
            endpointRequests = endpointRequests.entries.sortedBy { it.key.value }.associate { it.key to it.value.get() },
            upstream = upstreamSnapshot,
            cacheShutdownRejections = cacheSnapshot.totals.shutdownRejections,
            cacheWaiterCancellations = cacheSnapshot.totals.waiterCancellations,
            cacheActiveLoadInvalidations = cacheSnapshot.totals.activeLoadInvalidations,
            cacheStaleLoadCompletions = cacheSnapshot.totals.staleLoadCompletions,
            cacheNamespaces = MetricSnapshotMappers.cacheNamespaces(cacheSnapshot.namespaces),
            countersConsistent = updateSequence.isUnchangedAndStable(before) && upstreamSnapshot.countersConsistent &&
                cacheSnapshot.countersConsistent
        )
        return snapshot
    }

    fun record(duration: RequestDuration, endpoint: RequestEndpoint? = null) {
        updateSequence.withUpdate {
            requests.increment()
            totalDurationMillis.add(duration.value)
            endpoint?.let { endpointRequests.computeIfAbsent(endpointFor(it)) { MetricCounter() }.increment() }
        }
    }

    fun record(duration: Duration, endpoint: RequestEndpoint? = null) =
        record(RequestDuration(duration.inWholeMilliseconds), endpoint)

    fun recordRejected() = updateCounter { rejected.increment() }
    fun recordRetry() = updateCounter { retries.increment() }
    fun recordEviction() = updateCounter { evictions.increment() }
    fun recordCircuitOpening() = updateCounter { circuitOpenings.increment() }

    private inline fun updateCounter(update: () -> Unit) {
        updateSequence.withUpdate(update)
    }


    private fun endpointFor(endpoint: RequestEndpoint): RequestEndpoint {
        return if (endpoint.value == MetricNames.OTHER_ENDPOINT || endpointCardinality.admit(endpoint)) {
            endpoint
        } else {
            RequestEndpoint(MetricNames.OTHER_ENDPOINT)
        }
    }
}
