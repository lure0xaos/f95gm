package f95gm.server.app

import f95gm.domain.api.F95Api
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.request.requestPath
import f95gm.server.http.response.ApiBadRequestException
import f95gm.server.http.response.ApiRequestTooLargeException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*

internal fun ApplicationCall.isPublicDuringDrain(): Boolean =
    request.requestPath().value in setOf(F95Api.HEALTH, F95Api.SHUTDOWN)

internal fun ApplicationCall.validateApiContentLength(maxBytes: Long) {
    val raw = request.header(HttpHeaders.ContentLength) ?: return
    val length = raw.toLongOrNull()
    if (length == null || length < 0) throw ApiBadRequestException("Invalid Content-Length")
    if (length > maxBytes) throw ApiRequestTooLargeException()
}

internal fun ApplicationCall.requestIdHeader(): String? = request.header(ServerConstants.REQUEST_ID_HEADER)
