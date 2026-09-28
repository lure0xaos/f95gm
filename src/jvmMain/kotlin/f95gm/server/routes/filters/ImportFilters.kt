package f95gm.server.routes.filters

import f95gm.messages.UiMessages
import f95gm.server.model.ParseResult
import f95gm.server.http.request.receiveJsonResult
import f95gm.server.http.response.ApiFailure
import f95gm.server.http.response.ServerMessageKey
import f95gm.server.http.response.respondFailure
import f95gm.server.http.response.respondJson
import f95gm.server.model.filters.ImportedQuickLinksResponse
import f95gm.server.model.filters.QuickLinksBackup
import f95gm.domain.api.SavedFilterRequest
import f95gm.server.model.filters.SavedFilter
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.routes.filters.filterDependencies
import io.ktor.server.application.*

internal fun prepareImportedFilters(
    requests: List<SavedFilterRequest>,
    maxCount: Int,
    convert: (SavedFilterRequest) -> SavedFilter?
): List<SavedFilter> = requests.asSequence()
    .mapNotNull(convert)
    .distinctBy { it.name.value.trim().lowercase() }
    .take(maxCount)
    .toList()

internal suspend fun importFilters(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToSavedFilters()) ?: return
    val backupResult = call.receiveJsonResult<QuickLinksBackup>(call.filterDependencies.maxRequestBodyBytes.value).result
    val backup = (backupResult as? ParseResult.Success)?.value
    if (backup == null || backup.version != 1) {
        respondFailure(
            call,
            f95gm.server.config.constants.HttpStatusCatalog.BAD_REQUEST,
            ApiFailure(UiMessages.server_invalidQuickLinksBackup(), ServerMessageKey.INVALID_BACKUP)
        )
        return
    }
    val filters = prepareImportedFilters(
        backup.quickLinks,
        call.filterDependencies.filterRequestPolicy.maxImportedQuickLinks.value
    ) { request ->
        call.filterDependencies.applicationServices.savedFilters.fromRequest(request, call.filterDependencies.clock)
    }
    val result = call.filterDependencies.applicationServices.savedFilters.importAtomically(session.accountKey, filters)
    respondJson(call, f95gm.server.config.constants.HttpStatusCatalog.OK, ImportedQuickLinksResponse(result.imported))
}

