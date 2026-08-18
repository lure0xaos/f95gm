package f95gm.server.routes.filters

import f95gm.domain.api.F95HttpStatus
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.persistence.filters.backup.savedFiltersBackupJson
import f95gm.server.persistence.filters.loadSavedFilters
import f95gm.server.routes.session.browserSession
import io.ktor.server.application.*

internal suspend fun exportFilters(call: ApplicationCall) {
    val session = browserSession(call)?.second
    if (session == null) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_signInToSavedFilters()))
        return
    }
    respond(call, F95HttpStatus.SUCCESS_MIN, savedFiltersBackupJson(loadSavedFilters(session.accountKey)))
}
