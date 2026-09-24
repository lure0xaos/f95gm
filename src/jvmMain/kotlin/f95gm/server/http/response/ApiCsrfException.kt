package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import io.ktor.http.*

internal class ApiCsrfException(message: String = "Cross-site request rejected") :
    ApiApplicationException(HttpStatusCode.Forbidden, message, F95ApiErrorCode.CSRF_REJECTED)
