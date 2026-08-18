package f95gm.client.data.api

import f95gm.client.state.runtime.http
import f95gm.messages.UiMessages
import io.ktor.client.request.*
import io.ktor.client.statement.*

internal suspend fun deleteJson(path: String): String {
    val response = apiRequest(path) { http.delete(path) }
    val responseBody = response.bodyAsText()
    if (response.status.value !in f95gm.domain.api.F95HttpStatus.SUCCESS_MIN..f95gm.domain.api.F95HttpStatus.SUCCESS_MAX) {
        error(readApiError(responseBody, UiMessages.app_requestFailed(response.status.value)))
    }
    return responseBody
}
