package f95gm.server.routes.session

import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.auth.isAuthenticatedUpstreamResponse
import f95gm.server.http.response.ApiFailure
import f95gm.server.http.response.ServerMessageKeys
import f95gm.server.http.response.respondFailure
import f95gm.server.http.response.respondJson
import f95gm.server.model.http.OkResponse
import f95gm.server.state.runtime.routeDependencies
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.http.*
import io.ktor.server.application.*

internal suspend fun sessionStatus(call: ApplicationCall) {
    val current = browserSession(call, call.routeDependencies.sessions)
    if (current == null) {
        clearBrowserCookie(call)
        respondFailure(
            call,
            ServerConstants.STATUS_UNAUTHORIZED,
            ApiFailure(UiMessages.server_noActiveSession(), ServerMessageKeys.SESSION_MISSING)
        )
        return
    }

    val (id, session) = current
    val verified = call.routeDependencies.upstreamRequest(HttpMethod.Get, "/sam/latest_alpha/", session)
    if (isAuthenticatedUpstreamResponse(verified)) {
        respondJson(call, ServerConstants.STATUS_OK, OkResponse(true))
    } else {
        call.routeDependencies.sessionService.removeInMemory(id)
        call.routeDependencies.sessionService.remove(id)
        clearBrowserCookie(call)
        respondFailure(
            call,
            ServerConstants.STATUS_UNAUTHORIZED,
            ApiFailure(UiMessages.server_sessionExpired(), ServerMessageKeys.SESSION_EXPIRED)
        )
    }
}
