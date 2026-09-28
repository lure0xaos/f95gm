package f95gm.client.data.api

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import f95gm.domain.api.F95ApiMessageKeys
import f95gm.domain.api.ApiPath
import f95gm.domain.api.ApiErrorDetailsContract
import f95gm.client.model.runtime.HttpStatusCodeValue
import f95gm.client.model.runtime.ApiRequestId

internal open class ApiException(
    val path: ApiPath,
    val status: HttpStatusCodeValue? = null,
    message: String,
    val causeKind: ApiExceptionCause = ApiExceptionCause.NONE,
    val code: F95ApiErrorCode? = null,
    val messageKey: F95ApiMessageKey? = null,
    val details: ApiErrorDetailsContract = ApiErrorDetailsContract(),
    cause: Throwable? = null,
    var requestId: ApiRequestId? = null
) : RuntimeException(message, cause) {
    val retryable: Boolean
        get() = causeKind == ApiExceptionCause.CONNECTIVITY ||
            code == F95ApiErrorCode.RATE_LIMITED || code == F95ApiErrorCode.UPSTREAM_FAILURE
}

internal class ApiCsrfException(
    path: ApiPath,
    status: HttpStatusCodeValue,
    message: String,
    messageKey: F95ApiMessageKey? = null,
    details: ApiErrorDetailsContract = ApiErrorDetailsContract(),
    cause: Throwable? = null
) : ApiException(
    path = path, status = status, message = message, code = F95ApiErrorCode.CSRF_REJECTED,
    messageKey = messageKey, details = details, cause = cause
)

internal class ApiBadRequestException(
    path: ApiPath,
    status: HttpStatusCodeValue,
    message: String,
    code: F95ApiErrorCode = F95ApiErrorCode.INVALID_INPUT,
    messageKey: F95ApiMessageKey? = null,
    details: ApiErrorDetailsContract = ApiErrorDetailsContract(),
    cause: Throwable? = null
) : ApiException(path, status, message, code = code, messageKey = messageKey, details = details, cause = cause)

internal class ApiUpstreamException(
    path: ApiPath,
    status: HttpStatusCodeValue,
    message: String,
    messageKey: F95ApiMessageKey? = null,
    details: ApiErrorDetailsContract = ApiErrorDetailsContract(),
    cause: Throwable? = null
) : ApiException(
    path = path, status = status, message = message, code = F95ApiErrorCode.UPSTREAM_FAILURE,
    messageKey = messageKey, details = details, cause = cause
)

internal enum class ApiExceptionCause { NONE, CONNECTIVITY, PAYLOAD }

internal fun normalizeApiException(
    error: Throwable,
    path: ApiPath = ApiPath(""),
    causeKind: ApiExceptionCause = ApiExceptionCause.NONE
): ApiException = when (error) {
    is ApiException -> error
    else -> ApiException(
        path = path,
        message = error.message ?: apiErrorFallback,
        causeKind = when {
            causeKind != ApiExceptionCause.NONE -> causeKind
            error is PayloadParseException -> ApiExceptionCause.PAYLOAD
            else -> ApiExceptionCause.NONE
        },
        cause = error
    )
}

internal fun classifyApiFailure(exception: ApiException): ApiFailureKind = when {
    exception.causeKind == ApiExceptionCause.CONNECTIVITY -> ApiFailureKind.CONNECTIVITY
    exception.causeKind == ApiExceptionCause.PAYLOAD -> ApiFailureKind.PAYLOAD
    exception.code == F95ApiErrorCode.RATE_LIMITED -> ApiFailureKind.RATE_LIMITED
    exception.code == F95ApiErrorCode.INVALID_INPUT -> ApiFailureKind.VALIDATION
    exception.code == F95ApiErrorCode.UPSTREAM_FAILURE -> ApiFailureKind.UPSTREAM
    exception.status?.value == 401 || exception.messageKey in authenticationMessageKeys ->
        ApiFailureKind.AUTHENTICATION
    else -> ApiFailureKind.INTERNAL
}

private val authenticationMessageKeys = F95ApiMessageKeys.AUTHENTICATION

internal object ApiFailurePolicy {
    fun classify(exception: ApiException): ApiFailureKind = classifyApiFailure(exception)
    fun map(exception: ApiException): ApiFailure = ApiFailure(classify(exception), exception)
}

internal val ApiException.kind: ApiFailureKind
    get() = classifyApiFailure(this)

internal data class ApiFailure(
    val kind: ApiFailureKind,
    val exception: ApiException
) {
    val retryable: Boolean get() = exception.retryable
    fun userMessage(fallback: ApiFallback): String = exception.apiUserMessage(fallback)
}

internal val ApiException.failure: ApiFailure
    get() = ApiFailurePolicy.map(this)
