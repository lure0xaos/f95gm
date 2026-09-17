package f95gm.server.config.constants

import io.ktor.http.*

internal object ServerHttpStatus {
    val BadRequest = HttpStatusCode.BadRequest
    val Ok = HttpStatusCode.OK
    val Unauthorized = HttpStatusCode.Unauthorized
    val Forbidden = HttpStatusCode.Forbidden
    val NotFound = HttpStatusCode.NotFound
    val BadGateway = HttpStatusCode.BadGateway
}
