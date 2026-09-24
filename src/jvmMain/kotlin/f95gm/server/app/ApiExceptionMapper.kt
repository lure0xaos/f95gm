package f95gm.server.app

import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import f95gm.server.http.response.ApiApplicationException
import io.ktor.http.*
import java.io.IOException

internal data class MappedApiException(val status: HttpStatusCode, val error: F95ApiError)

internal fun mapApiException(error: Throwable): MappedApiException? = when (error) {
    is ApiApplicationException -> MappedApiException(
        error.status,
        F95ApiError(error.message ?: "Request failed", error.code, error.messageKey, error.details)
    )

    is IllegalArgumentException -> MappedApiException(
        HttpStatusCode.BadRequest,
        F95ApiError(
            error.message ?: "Invalid request",
            F95ApiErrorCode.INVALID_INPUT,
            F95ApiMessageKey("server.invalidRequest")
        )
    )

    is IOException -> MappedApiException(
        HttpStatusCode.BadGateway,
        F95ApiError(
            "Upstream request failed",
            F95ApiErrorCode.UPSTREAM_FAILURE,
            F95ApiMessageKey("server.upstreamFailure")
        )
    )

    else -> null
}
