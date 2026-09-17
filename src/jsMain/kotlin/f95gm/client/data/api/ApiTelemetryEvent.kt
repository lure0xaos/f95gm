package f95gm.client.data.api

import f95gm.client.model.runtime.HttpStatusCodeValue
import f95gm.client.model.runtime.ApiRequestId
import f95gm.client.model.runtime.ApiEndpointId
import f95gm.domain.api.ApiPath

internal sealed interface ApiTelemetryEvent {
    val path: ApiPath
    data class Response(override val path: ApiPath, val status: HttpStatusCodeValue, val requestId: ApiRequestId = ApiRequestId(0L), val endpoint: ApiEndpointId = ApiEndpointId(path.value)) : ApiTelemetryEvent
    data class Completed(override val path: ApiPath, val status: HttpStatusCodeValue, val durationMs: Long, val requestId: ApiRequestId = ApiRequestId(0L), val endpoint: ApiEndpointId = ApiEndpointId(path.value)) : ApiTelemetryEvent
    data class Failure(override val path: ApiPath, val kind: ApiFailureKind, val message: String, val requestId: ApiRequestId = ApiRequestId(0L), val endpoint: ApiEndpointId = ApiEndpointId(path.value)) : ApiTelemetryEvent
    data class Unavailable(override val path: ApiPath, val requestId: ApiRequestId = ApiRequestId(0L), val endpoint: ApiEndpointId = ApiEndpointId(path.value)) : ApiTelemetryEvent
}
