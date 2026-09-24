package f95gm.server.routes.filters

import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.request.decodeJsonOrNull
import f95gm.server.http.request.receiveLimitedText
import f95gm.server.http.response.ApiFailure
import f95gm.server.http.response.ServerMessageKeys
import f95gm.server.http.response.respondFailure
import f95gm.server.http.response.respondJson
import f95gm.server.model.filters.ImportedQuickLinksResponse
import f95gm.server.model.filters.QuickLinksBackup
import f95gm.server.persistence.filters.toSavedFilter
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.state.runtime.routeDependencies
import io.ktor.server.application.*

internal suspend fun importFilters(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToSavedFilters()) ?: return
    val backup = decodeJsonOrNull<QuickLinksBackup>(call.receiveLimitedText(ServerConstants.MAX_REQUEST_BODY_BYTES))
    if (backup == null || backup.version != 1) {
        respondFailure(
            call,
            ServerConstants.STATUS_BAD_REQUEST,
            ApiFailure(UiMessages.server_invalidQuickLinksBackup(), ServerMessageKeys.INVALID_BACKUP)
        )
        return
    }
    val imported = backup.quickLinks
        .distinctBy { it.name.value.trim().lowercase() }
        .take(ServerConstants.MAX_QUICK_LINKS)
        .mapNotNull { it.toSavedFilter(call.routeDependencies.clock) }
        .onEach { call.routeDependencies.applicationServices.savedFilters.store(session.accountKey, it) }
        .size
    respondJson(call, ServerConstants.STATUS_OK, ImportedQuickLinksResponse(imported))
}
