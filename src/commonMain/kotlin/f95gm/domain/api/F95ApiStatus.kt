package f95gm.domain.api

/** HTTP statuses that can appear in the shared API contract. */
enum class F95ApiStatus(val code: Int, val description: String) {
    OK(200, "Successful response"),
    ACCEPTED(202, "Successful response"),
    BAD_REQUEST(400, "Invalid request"),
    UNAUTHORIZED(401, "Authentication required"),
    FORBIDDEN(403, "Request forbidden"),
    NOT_FOUND(404, "Resource not found"),
    CONFLICT(409, "Request conflicts with current state"),
    PAYLOAD_TOO_LARGE(413, "Request body too large"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported request media type"),
    TOO_MANY_REQUESTS(429, "Rate limit exceeded"),
    INTERNAL_SERVER_ERROR(500, "Request failed"),
    BAD_GATEWAY(502, "Upstream request failed"),
    SERVICE_UNAVAILABLE(503, "Service unavailable"),
    GATEWAY_TIMEOUT(504, "Upstream request timed out");

    companion object {
        val DEFAULT_ERROR_STATUSES: Set<F95ApiStatus> = setOf(
            BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, PAYLOAD_TOO_LARGE,
            UNSUPPORTED_MEDIA_TYPE, TOO_MANY_REQUESTS, INTERNAL_SERVER_ERROR, BAD_GATEWAY,
            SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT
        )
    }
}
