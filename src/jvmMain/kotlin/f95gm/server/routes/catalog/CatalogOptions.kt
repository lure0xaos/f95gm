package f95gm.server.routes.catalog

import f95gm.domain.api.F95Api
import f95gm.domain.api.F95HttpStatus
import f95gm.domain.defaults.F95Defaults
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.filters.filterCategories
import f95gm.server.config.logging.logger
import f95gm.server.config.upstream.latestPage
import f95gm.server.config.upstream.upstream
import f95gm.server.http.auth.isAuthenticatedUpstreamResponse
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.model.catalog.CatalogOptionsResponse
import f95gm.server.model.catalog.LatestUpdatesConfig
import f95gm.server.routes.catalog.parsing.extractJavaScriptObject
import f95gm.server.routes.session.browserSession
import f95gm.server.state.runtime.upstreamJson
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.serialization.json.JsonArray

internal suspend fun catalogOptions(call: ApplicationCall) {
    val session = browserSession(call)?.second
    if (session == null) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_signInToFilters()))
        return
    }
    val category = call.request.queryParameters[F95Api.CATEGORY]
        ?.takeIf { it in filterCategories } ?: F95Defaults.DEFAULT_CATEGORY
    val result = upstreamRequest(
        method = "GET",
        path = latestPage.removePrefix(upstream),
        session = session,
        headers = mapOf(HttpHeaders.Referrer to latestPage, HttpHeaders.Accept to "text/html")
    )
    if (result.status !in F95HttpStatus.SUCCESS_MIN..F95HttpStatus.SUCCESS_MAX) {
        respond(call, result.status, jsonMessage(UiMessages.server_catalogOptionsMissing()))
        return
    }
    if (!isAuthenticatedUpstreamResponse(result)) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_signInAgain()))
        return
    }
    val rawConfig = extractJavaScriptObject(result.body, "latestUpdates")
    val config = rawConfig?.let { raw ->
        runCatching { upstreamJson.decodeFromString<LatestUpdatesConfig>(raw) }.getOrNull()
    }
    if (config == null) {
        val reason =
            if (rawConfig == null) "latestUpdates assignment was not found" else "latestUpdates JSON could not be decoded"
        logger.warn {
            "Unable to parse F95zone catalog options: $reason; upstreamBodyLength=${result.body.length}"
        }
        respond(call, ServerConstants.STATUS_BAD_GATEWAY, jsonMessage(UiMessages.server_catalogOptionsInvalid()))
        return
    }
    val prefixes = config.prefixes[category] ?: JsonArray(emptyList())
    respond(call, F95HttpStatus.SUCCESS_MIN, upstreamJson.encodeToString(CatalogOptionsResponse(config.tags, prefixes)))
}
