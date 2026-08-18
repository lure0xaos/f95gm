package f95gm.server.routes.filters

import f95gm.domain.api.F95HttpStatus
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.model.filters.SavedFilterRequest
import f95gm.server.persistence.filters.savedFiltersJson
import f95gm.server.persistence.filters.storeSavedFilter
import f95gm.server.persistence.filters.toSavedFilter
import f95gm.server.routes.session.browserSession
import f95gm.server.state.runtime.upstreamJson
import io.ktor.server.application.*
import io.ktor.server.request.*

internal suspend fun saveFilter(call: ApplicationCall) {
    val session = browserSession(call)?.second
    if (session == null) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_signInToSaveFilters()))
        return
    }

    val body = call.receiveText()
    val request = runCatching { upstreamJson.decodeFromString<SavedFilterRequest>(body) }.getOrNull()
    if (request == null) {
        respond(call, ServerConstants.STATUS_BAD_REQUEST, jsonMessage(UiMessages.server_invalidSavedFilterPayload()))
        return
    }
    val filter = request.toSavedFilter()
    if (filter == null) {
        respond(call, ServerConstants.STATUS_BAD_REQUEST, jsonMessage(UiMessages.server_savedFilterNameRequired()))
        return
    }
    val storedFilter = storeSavedFilter(session.accountKey, filter)
    respond(call, F95HttpStatus.SUCCESS_MIN, savedFiltersJson(listOf(storedFilter)))
}
