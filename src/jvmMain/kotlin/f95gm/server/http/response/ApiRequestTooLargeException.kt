package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import io.ktor.http.*

internal class ApiRequestTooLargeException(message: String = "Request body is too large") :
    ApiApplicationException(HttpStatusCode.PayloadTooLarge, message, F95ApiErrorCode.REQUEST_TOO_LARGE, kind = ApiFailureKind.Validation)
