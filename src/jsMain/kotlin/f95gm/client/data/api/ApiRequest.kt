package f95gm.client.data.api

import io.ktor.client.statement.*
import kotlinx.coroutines.CancellationException
import f95gm.client.model.runtime.HttpStatusCodeValue
import f95gm.client.model.runtime.ApiRequestId
import f95gm.client.model.runtime.ApiEndpointId
import f95gm.domain.api.ApiPath

internal suspend fun apiRequest(
    path: ApiPath,
    request: suspend (ApiTransport) -> HttpResponse,
    transport: ApiTransport,
    events: ApiClientEvents,
    requestId: ApiRequestId = ApiRequestId(0L),
    endpoint: ApiEndpointId = ApiEndpointId(path.value)
): HttpResponse = try {
        request(transport).also { response -> events.emit(ApiTelemetryEvent.Response(path, HttpStatusCodeValue(response.status.value), requestId, endpoint)) }
} catch (error: CancellationException) {
    throw error
} catch (error: ApiTransportException) {
    val failure = ApiFailureMapper.fromTransport(path, error)
        failure.requestId = requestId
        events.emit(ApiTelemetryEvent.Unavailable(path, requestId, endpoint))
        events.emit(ApiTelemetryEvent.Failure(path, failure.kind, failure.message ?: apiErrorFallback, requestId, endpoint))
    throw failure
}
