package f95gm.server.routes.marks

import f95gm.messages.UiMessages
import f95gm.server.http.response.respond
import f95gm.server.http.response.marksJson
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.routes.marks.markDependencies
import io.ktor.server.application.*

internal suspend fun checkedMarks(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToCheckMarked()) ?: return

    val refreshed = call.markDependencies.loadAndRefreshMarks(session)
    respond(call, f95gm.server.config.constants.HttpStatusCatalog.OK, marksJson(refreshed.marks, refreshed.live))
}
