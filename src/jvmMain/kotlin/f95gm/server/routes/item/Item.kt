package f95gm.server.routes.item

import f95gm.domain.api.F95HttpStatus
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.logging.logger
import f95gm.server.config.upstream.latestPage
import f95gm.server.config.upstream.upstream
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.routes.session.browserSession
import f95gm.server.upstream.parser.parseThreadDetail
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.http.*
import io.ktor.server.application.*
import org.jsoup.Jsoup

internal suspend fun item(call: ApplicationCall) {
    val session = browserSession(call)?.second
    if (session == null) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_signInToItem()))
        return
    }
    val threadId = call.parameters["threadId"]?.toLongOrNull()?.takeIf { it > 0 }
    if (threadId == null) {
        respond(call, ServerConstants.STATUS_BAD_REQUEST, jsonMessage(UiMessages.server_invalidItemId()))
        return
    }

    val headers = mapOf(
        HttpHeaders.Referrer to latestPage,
        HttpHeaders.Accept to "text/html,application/xhtml+xml"
    )
    val first = upstreamRequest("GET", "/threads/$threadId/", session, headers = headers)
    val result =
        if (first.status in ServerConstants.REDIRECT_STATUS_MIN..ServerConstants.REDIRECT_STATUS_MAX && first.location != null) {
            upstreamRequest(
                method = "GET",
                path = first.location,
                session = session,
                headers = headers
            )
        } else {
            first
        }
    if (result.status !in F95HttpStatus.SUCCESS_MIN..F95HttpStatus.SUCCESS_MAX) {
        logger.warn {
            "F95zone item request failed: threadId=$threadId, status=${result.status}, " +
                    "location=${result.location ?: "none"}, contentType=${result.contentType ?: "none"}, " +
                    "bodyLength=${result.body.length}, title=${
                        Jsoup.parse(result.body, upstream).title().ifBlank { "none" }
                    }"
        }
        val message = when (result.status) {
            ServerConstants.STATUS_FORBIDDEN -> UiMessages.server_accessDenied()
            ServerConstants.STATUS_NOT_FOUND -> UiMessages.server_itemNotFound()
            else -> UiMessages.server_itemLoadFailed(result.status)
        }
        respond(call, result.status, jsonMessage(message))
        return
    }

    val detail = parseThreadDetail(threadId.toString(), result.body)
    respond(call, F95HttpStatus.SUCCESS_MIN, detail.toJson())
}
