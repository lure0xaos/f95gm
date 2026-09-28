package f95gm.server.routes.catalog

import f95gm.domain.api.F95ApiQueryParameters
import f95gm.domain.defaults.F95Defaults
import f95gm.messages.UiMessages
import f95gm.server.config.filters.filterCategories
import f95gm.server.config.constants.HttpStatusCatalog
import f95gm.server.http.response.respondFailure
import f95gm.server.http.response.respondJsonCacheable
import f95gm.server.http.response.respondUpstreamFailure
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.routes.catalog.catalogDependencies
import io.ktor.server.application.*
import f95gm.server.http.request.get
import kotlin.time.Duration.Companion.seconds

internal suspend fun catalogOptions(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToFilters()) ?: return
    val dependencies = call.catalogDependencies
    val category = queryValue(call, F95ApiQueryParameters.CATEGORY)
        ?.takeIf { it in filterCategories } ?: F95Defaults.DEFAULT_CATEGORY
    when (val result = dependencies.catalogOptionsService.fetch(category, session)) {
        is CatalogOptionsResult.UpstreamFailure -> {
            if (call.respondUpstreamFailure(result.response)) return
            respondFailure(call, catalogUpstreamFallbackResponse(result.response))
        }
        CatalogOptionsResult.Unauthenticated, is CatalogOptionsResult.Invalid -> {
            respondFailure(call, result.failureResponse())
        }
        is CatalogOptionsResult.Success -> respondJsonCacheable(
            call,
            HttpStatusCatalog.OK,
            result.value,
            maxAge = 30.seconds
        )
    }
}
