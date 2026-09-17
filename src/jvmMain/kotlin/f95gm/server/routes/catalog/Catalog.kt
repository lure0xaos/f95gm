package f95gm.server.routes.catalog

import f95gm.messages.UiMessages
import f95gm.server.http.response.respondUpstreamFailure
import f95gm.server.http.response.respond
import f95gm.server.model.http.userFacingBody
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.routes.catalog.catalogDependencies
import f95gm.server.upstream.request.UpstreamPaths
import io.ktor.server.application.*

internal suspend fun catalog(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToCatalog()) ?: return
    val dependencies = call.catalogDependencies
    val request = call.catalogRequest(dependencies.catalogRequestPolicy)
    val query = CatalogQuery(
        request.category,
        request.page,
        request.rows,
        request.sort,
        dependencies.clock.epochMillis().value,
        request.tags,
        request.excludedTags,
        request.prefixes,
        request.excludedPrefixes,
        request.tagType,
        request.date,
        request.search,
        request.creator
    ).encoded()
    val result = dependencies.catalogService.fetch(UpstreamPaths.catalog(query, dependencies.upstreamEndpoints), session)
    if (!call.respondUpstreamFailure(result)) respond(call, result.status, result.userFacingBody, result.contentType)
}
