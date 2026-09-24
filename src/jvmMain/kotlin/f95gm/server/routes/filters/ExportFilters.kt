package f95gm.server.routes.filters

import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.response.respond
import f95gm.server.persistence.filters.backup.savedFiltersBackupJson
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.state.runtime.routeDependencies
import io.ktor.server.application.*

internal suspend fun exportFilters(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToSavedFilters()) ?: return
    respond(
        call,
        ServerConstants.STATUS_OK,
        savedFiltersBackupJson(call.routeDependencies.applicationServices.savedFilters.load(session.accountKey))
    )
}
