package f95gm.server.http.response

import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import f95gm.server.state.runtime.upstreamJson

internal fun jsonMessage(
    message: String,
    messageKey: F95ApiMessageKey? = F95ApiMessageKey("server.requestFailed"),
    code: F95ApiErrorCode = F95ApiErrorCode.INTERNAL,
    details: Map<String, String> = emptyMap()
): String = upstreamJson.encodeToString(F95ApiError(message, code, messageKey, details))
