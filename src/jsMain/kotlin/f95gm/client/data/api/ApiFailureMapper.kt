package f95gm.client.data.api

import f95gm.client.model.runtime.HttpStatusCodeValue
import f95gm.domain.api.ApiPath
import f95gm.domain.api.F95ApiErrorCode

/** Maps protocol failures into domain failures without deciding when they are user-visible. */
internal object ApiFailureMapper {
    fun fromHttp(path: ApiPath, status: HttpStatusCodeValue, body: String, fallback: String): ApiException {
        val apiError = parseApiError(body, fallback)
        val message = apiError.message
        return when (apiError.code) {
            F95ApiErrorCode.CSRF_REJECTED -> ApiCsrfException(
                path, status, message, apiError.messageKey, apiError.details
            )
            F95ApiErrorCode.INVALID_INPUT,
            F95ApiErrorCode.REQUEST_TOO_LARGE -> ApiBadRequestException(
                path, status, message, apiError.code, apiError.messageKey, apiError.details
            )
            F95ApiErrorCode.UPSTREAM_FAILURE -> ApiUpstreamException(
                path, status, message, apiError.messageKey, apiError.details
            )
            else -> ApiException(
                path = path,
                status = status,
                message = message,
                code = apiError.code,
                messageKey = apiError.messageKey,
                details = apiError.details
            )
        }
    }

    fun fromTransport(path: ApiPath, error: ApiTransportException): ApiException =
        normalizeApiException(error, path = path, causeKind = ApiExceptionCause.CONNECTIVITY)
}
