package f95gm.server.routes.marks

import f95gm.domain.api.F95HttpStatus
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.model.http.OkResponse
import f95gm.server.persistence.marks.removeMark
import f95gm.server.routes.session.browserSession
import f95gm.server.state.runtime.upstreamJson
import io.ktor.server.application.*

internal suspend fun deleteMark(call: ApplicationCall) {
    val session = browserSession(call)?.second
    if (session == null) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_signInToUnmark()))
        return
    }
    val threadId = call.parameters["threadId"]?.toLongOrNull()?.takeIf { it > 0 }?.toString()
    if (threadId == null) {
        respond(call, ServerConstants.STATUS_BAD_REQUEST, jsonMessage(UiMessages.server_invalidGameId()))
        return
    }
    removeMark(session.accountKey, threadId)
    respond(call, F95HttpStatus.SUCCESS_MIN, upstreamJson.encodeToString(OkResponse(true)))
}
