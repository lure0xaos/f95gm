package f95gm.server.routes.marks

import f95gm.domain.api.F95HttpStatus
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.persistence.marks.loadMarks
import f95gm.server.persistence.marks.marksJson
import f95gm.server.persistence.marks.resetTrackingStateForNewVersion
import f95gm.server.routes.marks.live.loadLiveMarks
import f95gm.server.routes.session.browserSession
import io.ktor.server.application.*

internal suspend fun marks(call: ApplicationCall) {
    val session = browserSession(call)?.second
    if (session == null) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_signInToMarked()))
        return
    }
    val marks = loadMarks(session.accountKey)
    val live = loadLiveMarks(marks, session)
    val updatedMarks = resetTrackingStateForNewVersion(session.accountKey, marks, live)
    respond(call, F95HttpStatus.SUCCESS_MIN, marksJson(updatedMarks, live))
}
