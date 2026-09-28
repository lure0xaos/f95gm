package f95gm.server.http.request

import f95gm.domain.api.UpstreamMetricsSnapshotContract
import f95gm.domain.api.CacheNamespaceMetricsContract
internal data class RequestMetricsSnapshot(
    val requests: Long,
    val rejected: Long,
    val totalDurationMillis: Long,
    val retries: Long,
    val evictions: Long,
    val circuitOpenings: Long,
    val endpointRequests: Map<RequestEndpoint, Long> = emptyMap(),
    val upstream: UpstreamMetricsSnapshotContract = UpstreamMetricsSnapshotContract(),
    val cacheShutdownRejections: Long = 0,
    val cacheWaiterCancellations: Long = 0,
    val cacheActiveLoadInvalidations: Long = 0,
    val cacheStaleLoadCompletions: Long = 0,
    val cacheNamespaces: Map<MetricNamespace, CacheNamespaceMetricsContract> = emptyMap(),
    val countersConsistent: Boolean = true
) {
    val averageDurationMillis: Double
        get() = if (requests == 0L) 0.0 else totalDurationMillis.toDouble() / requests
}

internal fun RequestMetricsSnapshot.toHttpResponse(): RequestMetricsHttpResponse = RequestMetricsHttpResponse(
    requests = requests,
    rejected = rejected,
    totalDurationMillis = totalDurationMillis,
    retries = retries,
    evictions = evictions,
    circuitOpenings = circuitOpenings,
    endpointRequests = endpointRequests.mapKeys { it.key.value },
    upstream = upstream,
    cacheShutdownRejections = cacheShutdownRejections,
    cacheWaiterCancellations = cacheWaiterCancellations,
    cacheActiveLoadInvalidations = cacheActiveLoadInvalidations,
    cacheStaleLoadCompletions = cacheStaleLoadCompletions,
    cacheNamespaces = cacheNamespaces.mapKeys { it.key.value },
    countersConsistent = countersConsistent
)

