package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import io.ktor.http.*

internal class ApiRateLimitedException(
    message: String = "Too many requests",
    details: Map<String, String> = emptyMap()
) : ApiApplicationException(
    HttpStatusCode.TooManyRequests,
    message,
    F95ApiErrorCode.RATE_LIMITED,
    details = details
)
