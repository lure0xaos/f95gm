package f95gm.server.http.request

import kotlinx.serialization.Serializable

@Serializable
internal data class RequestMetricsSnapshot(
    val requests: Long,
    val rejected: Long,
    val totalDurationMillis: Long,
    val retries: Long,
    val evictions: Long,
    val circuitOpenings: Long,
    val endpointRequests: Map<String, Long> = emptyMap()
) {
    val averageDurationMillis: Double
        get() = if (requests == 0L) 0.0 else totalDurationMillis.toDouble() / requests
}
