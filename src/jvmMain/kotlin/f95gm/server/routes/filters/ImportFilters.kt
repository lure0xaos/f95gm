package f95gm.server.routes.filters

import f95gm.domain.api.F95HttpStatus
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.model.filters.ImportedQuickLinksResponse
import f95gm.server.model.filters.QuickLinksBackup
import f95gm.server.persistence.filters.storeSavedFilter
import f95gm.server.persistence.filters.toSavedFilter
import f95gm.server.routes.session.browserSession
import f95gm.server.state.runtime.upstreamJson
import io.ktor.server.application.*
import io.ktor.server.request.*

internal suspend fun importFilters(call: ApplicationCall) {
    val session = browserSession(call)?.second
    if (session == null) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_signInToSavedFilters()))
        return
    }
    val backup = runCatching { upstreamJson.decodeFromString<QuickLinksBackup>(call.receiveText()) }.getOrNull()
    if (backup == null || backup.version != 1) {
        respond(call, ServerConstants.STATUS_BAD_REQUEST, jsonMessage(UiMessages.server_invalidQuickLinksBackup()))
        return
    }
    val imported = backup.quickLinks
        .distinctBy { it.name.value.trim().lowercase() }
        .take(ServerConstants.MAX_QUICK_LINKS)
        .mapNotNull { it.toSavedFilter() }
        .onEach { storeSavedFilter(session.accountKey, it) }
        .size
    respond(
        call,
        F95HttpStatus.SUCCESS_MIN,
        upstreamJson.encodeToString(ImportedQuickLinksResponse(imported))
    )
}
