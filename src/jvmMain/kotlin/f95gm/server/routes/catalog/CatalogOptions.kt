package f95gm.server.routes.catalog

import f95gm.domain.api.F95Api
import f95gm.domain.defaults.F95Defaults
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.filters.filterCategories
import f95gm.server.config.logging.logger
import f95gm.server.config.upstream.latestPage
import f95gm.server.config.upstream.upstream
import f95gm.server.http.auth.isAuthenticatedUpstreamResponse
import f95gm.server.http.response.*
import f95gm.server.model.catalog.CatalogOptionsResponse
import f95gm.server.model.catalog.LatestUpdatesConfig
import f95gm.server.routes.catalog.parsing.extractJavaScriptObject
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.state.runtime.routeDependencies
import f95gm.server.state.runtime.upstreamJson
import f95gm.server.upstream.request.UpstreamHeaders
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.serialization.json.JsonArray

internal suspend fun catalogOptions(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToFilters()) ?: return
    val category = call.request.queryParameters[F95Api.CATEGORY]
        ?.takeIf { it in filterCategories } ?: F95Defaults.DEFAULT_CATEGORY
    val result = call.routeDependencies.upstreamRequest(
        method = HttpMethod.Get,
        path = latestPage.removePrefix(upstream),
        session = session,
        headers = UpstreamHeaders.of(mapOf(HttpHeaders.Referrer to latestPage, HttpHeaders.Accept to "text/html"))
    )
    if (!result.status.isSuccessful) {
        if (call.respondUpstreamFailure(result)) return
        respondFailure(
            call,
            result.status,
            ApiFailure(UiMessages.server_catalogOptionsMissing(), ServerMessageKeys.CATALOG_OPTIONS_MISSING)
        )
        return
    }
    if (!isAuthenticatedUpstreamResponse(result)) {
        respondFailure(
            call,
            ServerConstants.STATUS_UNAUTHORIZED,
            ApiFailure(UiMessages.server_signInAgain(), ServerMessageKeys.SIGN_IN_AGAIN)
        )
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
        respondFailure(
            call,
            ServerConstants.STATUS_BAD_GATEWAY,
            ApiFailure(UiMessages.server_catalogOptionsInvalid(), ServerMessageKeys.CATALOG_OPTIONS_INVALID)
        )
        return
    }
    val prefixes = config.prefixes[category] ?: JsonArray(emptyList())
    respondJsonCacheable(
        call,
        ServerConstants.STATUS_OK,
        CatalogOptionsResponse(config.tags, prefixes),
        maxAgeSeconds = 30
    )
}
