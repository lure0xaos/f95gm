package f95gm.server.http.request

import f95gm.server.http.response.ApiFailure
import f95gm.server.http.response.ApiFailureDetails
import f95gm.server.http.response.ServerMessageKeys

internal fun RequestValidation.Invalid.toApiFailure(): ApiFailure = ApiFailure(
    message = message,
    key = messageKey ?: ServerMessageKeys.INVALID_JSON_REQUEST,
    code = code,
    details = field?.let(ApiFailureDetails::field) ?: ApiFailureDetails.EMPTY
)
