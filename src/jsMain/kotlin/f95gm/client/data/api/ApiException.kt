package f95gm.client.data.api

import f95gm.domain.api.F95ApiErrorCode

internal class ApiException(
    val path: String,
    val status: Int? = null,
    message: String,
    val connectivityFailure: Boolean = false,
    val code: F95ApiErrorCode? = null,
    val messageKey: String? = null,
    val details: Map<String, String> = emptyMap(),
    cause: Throwable? = null
) : RuntimeException(message, cause)

internal fun normalizeApiException(
    error: Throwable,
    path: String = "",
    connectivityFailure: Boolean = false
): ApiException = when (error) {
    is ApiException -> error
    else -> ApiException(
        path = path,
        message = error.message ?: apiErrorFallback,
        connectivityFailure = connectivityFailure,
        cause = error
    )
}

internal val ApiException.kind: ApiFailureKind
    get() = when {
        connectivityFailure -> ApiFailureKind.CONNECTIVITY
        code == F95ApiErrorCode.RATE_LIMITED -> ApiFailureKind.RATE_LIMITED
        code == F95ApiErrorCode.INVALID_INPUT -> ApiFailureKind.VALIDATION
        code == F95ApiErrorCode.UPSTREAM_FAILURE -> ApiFailureKind.UPSTREAM
        status == 401 || messageKey?.contains("session", ignoreCase = true) == true -> ApiFailureKind.AUTHENTICATION
        else -> ApiFailureKind.INTERNAL
    }
