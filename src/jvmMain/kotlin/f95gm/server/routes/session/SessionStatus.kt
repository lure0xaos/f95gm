package f95gm.server.routes.session

import f95gm.messages.UiMessages
import f95gm.server.http.auth.isAuthenticatedUpstreamResponse
import f95gm.server.http.response.ApiFailure
import f95gm.server.http.response.ServerMessageKey
import f95gm.server.http.response.respondFailure
import f95gm.server.http.response.respondJson
import f95gm.server.model.http.OkResponse
import f95gm.server.routes.session.sessionDependencies
import f95gm.server.upstream.request.UpstreamRequests
import f95gm.server.upstream.request.UpstreamPaths
import f95gm.server.upstream.request.UpstreamEndpointPolicy
import io.ktor.http.*
import io.ktor.server.application.*
import f95gm.server.config.constants.HttpStatusCatalog

internal suspend fun sessionStatus(call: ApplicationCall) {
    val current = browserSession(call, call.sessionDependencies.sessions)
    if (current !is BrowserSession.Found) {
        clearBrowserCookie(call)
        respondFailure(
            call,
            HttpStatusCatalog.UNAUTHORIZED,
            ApiFailure(UiMessages.server_noActiveSession(), ServerMessageKey.SESSION_MISSING)
        )
        return
    }

    val id = current.id
    val session = current.session
    val verified = call.sessionDependencies.upstream.request(
        UpstreamRequests.get(UpstreamPaths.sessionStatus(call.sessionDependencies.upstreamEndpoints), session, policy = UpstreamEndpointPolicy.LOGIN)
    )
    if (isAuthenticatedUpstreamResponse(verified, call.sessionDependencies.upstreamEndpoints.origin)) {
        respondJson(call, HttpStatusCatalog.OK, OkResponse(true))
    } else {
        call.sessionDependencies.sessionService.removeInMemory(id)
        call.sessionDependencies.sessionService.remove(id)
        clearBrowserCookie(call)
        respondFailure(
            call,
            HttpStatusCatalog.UNAUTHORIZED,
            ApiFailure(UiMessages.server_sessionExpired(), ServerMessageKey.SESSION_EXPIRED)
        )
    }
}

