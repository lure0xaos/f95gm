package f95gm.server.http.request

import f95gm.server.http.response.ApiFailure
import f95gm.server.http.response.ApiFailureDetails
import f95gm.server.http.response.ServerMessageKey

internal fun RequestValidation.Invalid.toApiFailure(): ApiFailure = ApiFailure(
    message = message,
    key = messageKey ?: ServerMessageKey.INVALID_JSON_REQUEST,
    code = code,
    details = field?.let(ApiFailureDetails::field) ?: ApiFailureDetails.EMPTY
)

