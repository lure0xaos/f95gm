package f95gm.client.data.api

import f95gm.client.actions.runtime.recordApiResponse
import f95gm.client.actions.runtime.recordJvmUnavailable
import io.ktor.client.statement.*
import kotlinx.coroutines.CancellationException

internal suspend fun apiRequest(
    path: String,
    request: suspend (ApiTransport) -> HttpResponse,
    transport: ApiTransport
): HttpResponse = try {
    request(transport).also { response -> recordApiResponse(path, response.status.value) }
} catch (error: CancellationException) {
    throw error
} catch (error: ApiTransportException) {
    recordJvmUnavailable()
    throw normalizeApiException(error, path = path, connectivityFailure = true)
} catch (error: Exception) {
    throw normalizeApiException(error, path = path, connectivityFailure = false)
}
