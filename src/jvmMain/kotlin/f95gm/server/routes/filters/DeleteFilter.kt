package f95gm.server.routes.filters

import f95gm.domain.api.F95HttpStatus
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.model.http.OkResponse
import f95gm.server.persistence.filters.removeSavedFilter
import f95gm.server.routes.filters.validation.isValidSavedFilterId
import f95gm.server.routes.session.browserSession
import f95gm.server.state.runtime.upstreamJson
import io.ktor.server.application.*

internal suspend fun deleteFilter(call: ApplicationCall) {
    val session = browserSession(call)?.second
    if (session == null) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_signInToDeleteFilters()))
        return
    }
    val filterId = call.parameters["filterId"]?.takeIf(::isValidSavedFilterId)
    if (filterId == null) {
        respond(call, ServerConstants.STATUS_BAD_REQUEST, jsonMessage(UiMessages.server_invalidSavedFilterId()))
        return
    }
    removeSavedFilter(session.accountKey, filterId)
    respond(call, F95HttpStatus.SUCCESS_MIN, upstreamJson.encodeToString(OkResponse(true)))
}
