package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import f95gm.server.kernel.ApiFailureMarker
import io.ktor.http.*

internal sealed interface ApiFailureKind {
    data object Validation : ApiFailureKind
    data object Authentication : ApiFailureKind
    data object Conflict : ApiFailureKind
    data object Upstream : ApiFailureKind
    data object RateLimit : ApiFailureKind
    data object Internal : ApiFailureKind
}

/** Base for concrete API failures; callers must choose a meaningful failure type. */
internal abstract class ApiApplicationException(
    val status: HttpStatusCode,
    message: String,
    val code: F95ApiErrorCode = F95ApiErrorCode.INTERNAL,
    val messageKey: F95ApiMessageKey? = null,
    val details: ApiFailureDetails = ApiFailureDetails.EMPTY,
    val kind: ApiFailureKind = ApiFailureKind.Internal
) : RuntimeException(message), ApiFailureMarker
