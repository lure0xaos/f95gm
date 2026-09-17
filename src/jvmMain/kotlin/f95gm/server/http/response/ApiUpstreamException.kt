package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import io.ktor.http.*

internal class ApiUpstreamException(
    message: String = "Upstream request failed",
    details: ApiFailureDetails = ApiFailureDetails.EMPTY
) : ApiApplicationException(
    HttpStatusCode.BadGateway,
    message,
    F95ApiErrorCode.UPSTREAM_FAILURE,
    F95ApiMessageKey("server.upstreamFailure"),
    details,
    kind = ApiFailureKind.Upstream
)
