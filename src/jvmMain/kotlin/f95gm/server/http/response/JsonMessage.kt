package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import f95gm.server.http.serialization.upstreamJson

internal fun jsonMessage(failure: ApiFailure): String =
    upstreamJson.encodeToString(ApiErrorWireMapper.fromFailure(failure))

internal fun jsonMessage(
    message: String,
    messageKey: F95ApiMessageKey? = F95ApiMessageKey("server.requestFailed"),
    code: F95ApiErrorCode = F95ApiErrorCode.INTERNAL,
    details: ApiFailureDetails
): String = upstreamJson.encodeToString(ApiErrorWireMapper.fromParts(message, code, messageKey, details))
