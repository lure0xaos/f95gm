package f95gm.client.data.api

import f95gm.domain.api.ApiResponseEnvelope
import f95gm.domain.api.ApiPath

internal data class ApiEndpointDescriptor<T>(
    val operation: GeneratedF95ApiOperation,
    val decode: (String) -> T,
    val fallback: ApiFallbackMessage,
    val retryPolicy: RetryPolicy = RetryPolicy()
)

internal suspend fun <T> ApiEndpointDescriptor<T>.execute(
    executor: ApiRequestExecutor,
    path: ApiPath = operation.path,
    body: ApiRequestBody? = null
): ApiResponseEnvelope<T> = executeOperation(
    operation, path, body, executor, ApiStatusFallback(fallback::resolve), retryPolicy
).mapBody(decode)
