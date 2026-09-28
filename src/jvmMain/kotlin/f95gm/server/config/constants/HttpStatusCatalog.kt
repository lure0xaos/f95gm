package f95gm.server.config.constants

import io.ktor.http.HttpStatusCode

/** Statuses used by the server boundary, kept separate from storage and domain limits. */
internal object HttpStatusCatalog {
    val OK = HttpStatusCode.OK
    val ACCEPTED = HttpStatusCode.Accepted
    val BAD_REQUEST = HttpStatusCode.BadRequest
    val UNAUTHORIZED = HttpStatusCode.Unauthorized
    val FORBIDDEN = HttpStatusCode.Forbidden
    val NOT_FOUND = HttpStatusCode.NotFound
    val CONFLICT = HttpStatusCode.Conflict
    val BAD_GATEWAY = HttpStatusCode.BadGateway
    val GATEWAY_TIMEOUT = HttpStatusCode.GatewayTimeout
    val SERVICE_UNAVAILABLE = HttpStatusCode.ServiceUnavailable
    val TOO_MANY_REQUESTS = HttpStatusCode.TooManyRequests
    val PAYLOAD_TOO_LARGE = HttpStatusCode.PayloadTooLarge
    val UNSUPPORTED_MEDIA_TYPE = HttpStatusCode.UnsupportedMediaType
}
