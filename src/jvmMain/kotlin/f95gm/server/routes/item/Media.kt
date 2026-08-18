package f95gm.server.routes.item

import f95gm.domain.api.F95HttpStatus
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.upstream.mediaHosts
import f95gm.server.config.upstream.upstream
import f95gm.server.config.upstream.upstreamUserAgent
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.routes.session.browserSession
import f95gm.server.state.runtime.client
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.net.URI

internal suspend fun media(call: ApplicationCall) {
    val session = browserSession(call)?.second
    if (session == null) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_signInToMedia()))
        return
    }
    val rawUrl = call.request.queryParameters["url"]
    val uri = runCatching { rawUrl?.let(::URI) }.getOrNull()
    if (uri == null || uri.scheme != "https" || uri.host !in mediaHosts) {
        respond(call, ServerConstants.STATUS_BAD_REQUEST, jsonMessage(UiMessages.server_mediaUrlNotAllowed()))
        return
    }

    val response = client.request(uri.toString()) {
        header(HttpHeaders.UserAgent, upstreamUserAgent)
        header(HttpHeaders.Cookie, session.header())
        header(HttpHeaders.Referrer, "$upstream/")
        header(HttpHeaders.Accept, "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
    }
    if (response.status.value !in F95HttpStatus.SUCCESS_MIN..F95HttpStatus.SUCCESS_MAX) {
        respond(call, response.status.value, jsonMessage(UiMessages.server_mediaStatus(response.status.value)))
        return
    }
    call.respondBytes(
        bytes = response.body<ByteArray>(),
        contentType = response.headers[HttpHeaders.ContentType]?.let(ContentType::parse),
        status = response.status
    )
}
