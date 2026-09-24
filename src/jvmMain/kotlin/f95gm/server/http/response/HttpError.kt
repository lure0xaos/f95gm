package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import io.ktor.http.*

internal class HttpError(
    status: HttpStatusCode,
    message: String,
    code: F95ApiErrorCode = F95ApiErrorCode.INTERNAL,
    messageKey: F95ApiMessageKey? = null,
    details: Map<String, String> = emptyMap()
) : ApiApplicationException(status, message, code, messageKey, details)
