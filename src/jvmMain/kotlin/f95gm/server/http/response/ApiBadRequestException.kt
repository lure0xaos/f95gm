package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import io.ktor.http.*

internal class ApiBadRequestException(
    message: String,
    messageKey: F95ApiMessageKey? = null,
    details: ApiFailureDetails = ApiFailureDetails.EMPTY
) : ApiApplicationException(HttpStatusCode.BadRequest, message, F95ApiErrorCode.INVALID_INPUT, messageKey, details, ApiFailureKind.Validation)
