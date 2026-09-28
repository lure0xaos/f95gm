package f95gm.client.data.api

import f95gm.domain.api.ApiResponseEnvelope
import f95gm.domain.api.ApiPath
import f95gm.client.model.runtime.HttpStatusCodeValue
import f95gm.client.model.runtime.ApiEndpointId
import io.ktor.http.*

internal suspend fun requestJson(
    path: ApiPath,
    method: HttpMethod,
    body: ApiRequestBody? = null,
    contentType: ApiContentType? = null,
    headers: ApiHeaders = ApiHeaders.EMPTY,
    fallback: ApiStatusFallback,
    endpoint: ApiEndpointId = ApiEndpointId(path.value),
    transport: ApiTransport,
    events: ApiClientEvents,
    executor: ApiRequestExecutor? = null,
    retryPolicy: RetryPolicy = RetryPolicy()
): String = requestJsonEnvelope(path, method, body, contentType, headers, fallback, endpoint, transport, events, executor, retryPolicy).body

internal suspend fun requestJsonEnvelope(
    path: ApiPath,
    method: HttpMethod,
    body: ApiRequestBody? = null,
    contentType: ApiContentType? = null,
    headers: ApiHeaders = ApiHeaders.EMPTY,
    fallback: ApiStatusFallback,
    endpoint: ApiEndpointId = ApiEndpointId(path.value),
    transport: ApiTransport,
    events: ApiClientEvents,
    executor: ApiRequestExecutor? = null,
    retryPolicy: RetryPolicy = RetryPolicy()
): ApiResponseEnvelope<String> {
    return (executor ?: ApiRequestExecutor(transport, events)).execute(
        ApiRequestContext(path = path, endpoint = endpoint, method = method, body = body, contentType = contentType, headers = headers, fallback = fallback, retryPolicy = retryPolicy)
    )
}
