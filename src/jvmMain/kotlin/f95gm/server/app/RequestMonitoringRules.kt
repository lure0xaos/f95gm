package f95gm.server.app

import f95gm.domain.api.F95ApiPaths
import f95gm.server.config.protocol.HttpProtocolConfig
import f95gm.server.http.request.requestPath
import f95gm.server.http.response.ApiBadRequestException
import f95gm.server.http.response.ApiRequestTooLargeException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import f95gm.domain.api.parseNonNegativeLong

internal fun ApplicationCall.isPublicDuringDrain(): Boolean =
    request.requestPath().value in setOf(F95ApiPaths.HEALTH, F95ApiPaths.SHUTDOWN)

internal fun ApplicationCall.validateApiContentLength(maxBytes: Long) {
    val raw = request.header(HttpHeaders.ContentLength) ?: return
    val length = raw.parseNonNegativeLong() ?: throw ApiBadRequestException("Invalid Content-Length")
    if (length > maxBytes) throw ApiRequestTooLargeException()
}

internal fun ApplicationCall.requestIdHeader(): String? = request.header(HttpProtocolConfig.REQUEST_ID_HEADER)
