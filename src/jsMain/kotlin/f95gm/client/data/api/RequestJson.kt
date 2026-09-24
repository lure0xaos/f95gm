package f95gm.client.data.api

import f95gm.client.state.runtime.http
import f95gm.domain.api.F95HttpStatus
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

internal suspend fun requestJson(
    path: String,
    method: HttpMethod,
    body: String? = null,
    fallback: (status: Int) -> String,
    transport: ApiTransport = ApiTransport(http)
): String {
    val response = apiRequest(path, transport = transport, request = { apiTransport ->
        apiTransport.request(path) {
            this.method = method
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    })
    val responseBody = response.bodyAsText()
    if (response.status.value !in F95HttpStatus.SUCCESS_MIN..F95HttpStatus.SUCCESS_MAX) {
        val apiError = parseApiError(responseBody, fallback(response.status.value))
        throw ApiException(
            path = path,
            status = response.status.value,
            message = apiError.message,
            code = apiError.code,
            messageKey = apiError.messageKey,
            details = apiError.details
        )
    }
    return responseBody
}
