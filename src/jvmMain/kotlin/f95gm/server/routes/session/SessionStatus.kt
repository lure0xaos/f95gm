package f95gm.server.routes.session

import f95gm.domain.api.F95HttpStatus
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.auth.isAuthenticatedUpstreamResponse
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.model.http.OkResponse
import f95gm.server.persistence.sessions.removePersistedSession
import f95gm.server.state.runtime.sessions
import f95gm.server.state.runtime.upstreamJson
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.server.application.*

internal suspend fun sessionStatus(call: ApplicationCall) {
    val current = browserSession(call)
    if (current == null) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_noActiveSession()))
        return
    }

    val (id, session) = current
    val verified = upstreamRequest("GET", "/sam/latest_alpha/", session)
    if (isAuthenticatedUpstreamResponse(verified)) {
        respond(call, F95HttpStatus.SUCCESS_MIN, upstreamJson.encodeToString(OkResponse(true)))
    } else {
        sessions.remove(id)
        removePersistedSession(id)
        clearBrowserCookie(call)
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_sessionExpired()))
    }
}
