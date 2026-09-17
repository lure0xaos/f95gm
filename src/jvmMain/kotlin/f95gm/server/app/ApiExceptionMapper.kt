package f95gm.server.app

import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import f95gm.server.http.response.ApiApplicationException
import f95gm.server.http.response.ApiErrorWireMapper
import f95gm.server.http.response.ApiFailureDetails
import f95gm.server.kernel.ServerInvariantViolation
import f95gm.server.observability.ApiFailureEvent
import f95gm.server.observability.logger
import f95gm.server.observability.structuredLog
import f95gm.server.observability.ExceptionType
import f95gm.server.model.http.HttpStatusValue
import io.ktor.http.*
import java.io.IOException

internal data class MappedApiException(val status: HttpStatusCode, val error: F95ApiError)

internal fun mapApiException(error: Throwable): MappedApiException? = when (error) {
    is ApiApplicationException -> mapped(error.status, ApiErrorPolicy.code(error), error.messageKey, error)

    is IllegalArgumentException -> mapped(HttpStatusCode.BadRequest, F95ApiErrorCode.INVALID_INPUT, F95ApiMessageKey("server.invalidRequest"), error)

    is IOException -> mapped(HttpStatusCode.BadGateway, F95ApiErrorCode.UPSTREAM_FAILURE, F95ApiMessageKey("server.upstreamFailure"), error)

    is ServerInvariantViolation -> mapped(
        HttpStatusCode.InternalServerError,
        F95ApiErrorCode.INTERNAL,
        F95ApiMessageKey("server.requestFailed"),
        error
    )

    else -> null
}

private fun mapped(
    status: HttpStatusCode,
    code: F95ApiErrorCode,
    messageKey: F95ApiMessageKey?,
    error: Throwable
): MappedApiException {
    logger.warn {
        structuredLog(
            ApiFailureEvent(HttpStatusValue(status.value), code, messageKey, ExceptionType(error::class.simpleName ?: "Throwable"))
        )
    }
    val details = (error as? ApiApplicationException)?.details ?: ApiFailureDetails.EMPTY
    return MappedApiException(status, ApiErrorWireMapper.fromParts(error.message ?: "Request failed", code, messageKey, details))
}
