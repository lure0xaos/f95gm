package f95gm.client.data.api

import f95gm.client.actions.runtime.recordApiResponse
import f95gm.client.actions.runtime.recordJvmUnavailable
import io.ktor.client.statement.*
import kotlinx.coroutines.CancellationException

internal suspend fun apiRequest(path: String, request: suspend () -> HttpResponse): HttpResponse = try {
    request().also { response -> recordApiResponse(path, response.status.value) }
} catch (error: Throwable) {
    if (error is CancellationException) throw error
    recordJvmUnavailable()
    throw error
}
