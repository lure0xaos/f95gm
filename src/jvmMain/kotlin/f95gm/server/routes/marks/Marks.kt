package f95gm.server.routes.marks

import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.response.respond
import f95gm.server.persistence.marks.marksJson
import f95gm.server.persistence.marks.resetTrackingStateForNewVersion
import f95gm.server.routes.marks.live.loadLiveMarks
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.state.runtime.routeDependencies
import io.ktor.server.application.*

internal suspend fun marks(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToMarked()) ?: return
    val marks = call.routeDependencies.applicationServices.marks.load(session.accountKey)
    val live = loadLiveMarks(call.routeDependencies, marks, session)
    val updatedMarks = resetTrackingStateForNewVersion(
        session.accountKey,
        marks,
        live,
        call.routeDependencies.applicationServices.marks
    )
    respond(call, ServerConstants.STATUS_OK, marksJson(updatedMarks, live))
}
