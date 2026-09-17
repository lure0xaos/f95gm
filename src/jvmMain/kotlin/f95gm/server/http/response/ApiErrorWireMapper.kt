package f95gm.server.http.response

import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey

/** Single wire-boundary mapper for typed server failures. */
internal object ApiErrorWireMapper {
    fun fromFailure(failure: ApiFailure): F95ApiError = fromParts(
        message = failure.message,
        code = failure.code,
        messageKey = failure.key.apiKey,
        details = failure.details
    )

    fun fromParts(
        message: String,
        code: F95ApiErrorCode,
        messageKey: F95ApiMessageKey?,
        details: ApiFailureDetails = ApiFailureDetails.EMPTY
    ): F95ApiError = F95ApiError.fromDetails(message, code, messageKey, details.asContract())

}
