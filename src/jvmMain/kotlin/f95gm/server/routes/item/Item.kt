package f95gm.server.routes.item

import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.logging.logger
import f95gm.server.config.upstream.latestPage
import f95gm.server.config.upstream.upstream
import f95gm.server.http.request.positiveThreadIdParameter
import f95gm.server.http.response.*
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.state.runtime.routeDependencies
import f95gm.server.upstream.parser.parseThreadDetail
import f95gm.server.upstream.request.UpstreamHeaders
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.http.*
import io.ktor.server.application.*
import org.jsoup.Jsoup

internal suspend fun item(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToItem()) ?: return
    val threadId = call.positiveThreadIdParameter("threadId")
    if (threadId == null) {
        respondApiFailure(
            call,
            ServerConstants.STATUS_BAD_REQUEST,
            UiMessages.server_invalidItemId(),
            ServerMessageKeys.INVALID_ITEM_ID
        )
        return
    }

    val headers = UpstreamHeaders.of(
        mapOf(
        HttpHeaders.Referrer to latestPage,
        HttpHeaders.Accept to "text/html,application/xhtml+xml"
        )
    )
    val first = call.routeDependencies.upstreamRequest(
        HttpMethod.Get,
        "/threads/${threadId.value}/",
        session,
        headers = headers
    )
    val location = first.location
    val result =
        if (first.status.isRedirect && location != null) {
            call.routeDependencies.upstreamRequest(
                method = HttpMethod.Get,
                path = location,
                session = session,
                headers = headers
            )
        } else {
            first
        }
    if (call.respondUpstreamFailure(result)) return
    if (!result.status.isSuccessful) {
        logger.warn {
            "F95zone item request failed: threadId=$threadId, status=${result.status}, " +
                    "location=${result.location ?: "none"}, contentType=${result.contentType ?: "none"}, " +
                    "bodyLength=${result.body.length}, title=${
                        Jsoup.parse(result.body, upstream).title().ifBlank { "none" }
                    }"
        }
        val message = when (result.status.value) {
            ServerConstants.STATUS_FORBIDDEN.value -> UiMessages.server_accessDenied()
            ServerConstants.STATUS_NOT_FOUND.value -> UiMessages.server_itemNotFound()
            else -> UiMessages.server_itemLoadFailed(result.status.value)
        }
        respondApiFailure(call, result.status, message, ServerMessageKeys.INVALID_ITEM_LOAD)
        return
    }

    val detail = parseThreadDetail(threadId, result.body)
    respondJson(call, ServerConstants.STATUS_OK, detail)
}
