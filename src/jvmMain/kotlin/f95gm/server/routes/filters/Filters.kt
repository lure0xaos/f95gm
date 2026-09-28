package f95gm.server.routes.filters

import f95gm.messages.UiMessages
import f95gm.server.http.response.respond
import f95gm.server.http.response.savedFiltersJson
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.routes.filters.filterDependencies
import io.ktor.server.application.*

internal suspend fun filters(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToSavedFilters()) ?: return
    respond(
        call,
        f95gm.server.config.constants.HttpStatusCatalog.OK,
        savedFiltersJson(call.filterDependencies.applicationServices.savedFilters.load(session.accountKey))
    )
}
