package f95gm.client.data.api

import f95gm.domain.api.ApiResponseEnvelope
import f95gm.domain.api.ApiStatusCode
import f95gm.domain.api.F95HttpStatus
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import f95gm.client.model.runtime.HttpStatusCodeValue
import f95gm.domain.api.ApiPath

/** Converts an HTTP response into the transport-neutral API envelope or domain failure. */
internal class ApiResponseDecoder {
    suspend fun decode(
        path: ApiPath,
        response: HttpResponse,
        fallback: ApiStatusFallback
    ): ApiResponseEnvelope<String> {
        val responseBody = response.bodyAsText()
        if (response.status.value !in F95HttpStatus.SUCCESS_MIN..F95HttpStatus.SUCCESS_MAX) {
            val status = HttpStatusCodeValue(response.status.value)
            throw ApiFailureMapper.fromHttp(path, status, responseBody, fallback.resolve(status))
        }
        return ApiResponseEnvelope(ApiStatusCode(response.status.value), responseBody)
    }
}
