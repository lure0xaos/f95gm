package f95gm.client.state.runtime

import f95gm.client.data.api.ApiTelemetryEvent
import f95gm.client.model.runtime.ApiRequestId

internal data class ApiTelemetryState(
    val lastOperation: String = "",
    val lastStatus: Int? = null,
    val lastDurationMs: Long = 0,
    val lastRequestId: ApiRequestId = ApiRequestId(0L),
    val requestCount: Int = 0,
    val failureCount: Int = 0,
    val inFlightRequestIds: Set<ApiRequestId> = emptySet(),
    val lastFailureKind: f95gm.client.data.api.ApiFailureKind? = null,
    val lastFailureMessage: String = "",
    val endpointRequests: Map<String, Int> = emptyMap()
)

internal fun ApiTelemetryState.reduce(event: ApiTelemetryEvent): ApiTelemetryState = when (event) {
    is ApiTelemetryEvent.Response -> copy(
        inFlightRequestIds = inFlightRequestIds + event.requestId
    )
    is ApiTelemetryEvent.Completed -> copy(
        lastOperation = event.path.value,
        lastStatus = event.status.value,
        lastDurationMs = event.durationMs,
        lastRequestId = event.requestId,
        requestCount = requestCount + 1,
        inFlightRequestIds = inFlightRequestIds - event.requestId,
        endpointRequests = endpointRequests + (event.endpoint.value to ((endpointRequests[event.endpoint.value] ?: 0) + 1))
    )
    is ApiTelemetryEvent.Failure -> copy(
        failureCount = failureCount + 1,
        lastFailureKind = event.kind,
        lastFailureMessage = event.message,
        inFlightRequestIds = inFlightRequestIds - event.requestId
    )
    is ApiTelemetryEvent.Unavailable -> copy(inFlightRequestIds = inFlightRequestIds - event.requestId)
}
