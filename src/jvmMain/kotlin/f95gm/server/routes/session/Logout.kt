package f95gm.server.routes.session

import f95gm.domain.api.F95HttpStatus
import f95gm.server.http.response.respond
import f95gm.server.model.http.OkResponse
import f95gm.server.persistence.sessions.removePersistedSession
import f95gm.server.state.runtime.sessions
import f95gm.server.state.runtime.upstreamJson
import io.ktor.server.application.*

internal suspend fun logout(call: ApplicationCall) {
    browserSession(call)?.let {
        sessions.remove(it.first)
        removePersistedSession(it.first)
    }
    clearBrowserCookie(call)
    respond(call, F95HttpStatus.SUCCESS_MIN, upstreamJson.encodeToString(OkResponse(true)))
}
