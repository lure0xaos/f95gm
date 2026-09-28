package f95gm.server.http.request

import f95gm.domain.api.CacheNamespaceMetricsContract
import f95gm.domain.api.UpstreamMetricsSnapshotContract
import kotlinx.serialization.Serializable

@Serializable
internal data class RequestMetricsHttpResponse(
    val requests: Long,
    val rejected: Long,
    val totalDurationMillis: Long,
    val retries: Long,
    val evictions: Long,
    val circuitOpenings: Long,
    val endpointRequests: Map<String, Long> = emptyMap(),
    val upstream: UpstreamMetricsSnapshotContract = UpstreamMetricsSnapshotContract(),
    val cacheShutdownRejections: Long = 0,
    val cacheWaiterCancellations: Long = 0,
    val cacheActiveLoadInvalidations: Long = 0,
    val cacheStaleLoadCompletions: Long = 0,
    val cacheNamespaces: Map<String, CacheNamespaceMetricsContract> = emptyMap(),
    val countersConsistent: Boolean = true
) {
    val averageDurationMillis: Double
        get() = if (requests == 0L) 0.0 else totalDurationMillis.toDouble() / requests
}

