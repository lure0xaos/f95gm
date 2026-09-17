package f95gm.server.routes.catalog

import f95gm.server.observability.logger
import f95gm.server.config.protocol.UpstreamProtocolConfig
import f95gm.server.http.auth.isAuthenticatedUpstreamResponse
import f95gm.server.model.ParseResult
import f95gm.server.http.request.decodeJsonResult
import f95gm.server.http.response.isSuccessful
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.catalog.CatalogOptionsResponse
import f95gm.server.model.catalog.LatestUpdatesConfig
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.routes.catalog.parsing.extractJavaScriptObject
import f95gm.server.routes.ports.UpstreamRouteDependencies
import f95gm.server.upstream.request.UpstreamEndpointPolicy
import f95gm.server.upstream.request.UpstreamHeaders
import f95gm.server.upstream.request.UpstreamHeader
import f95gm.server.upstream.request.UpstreamPaths
import f95gm.server.upstream.request.UpstreamRequests
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.JsonArray

internal sealed interface CatalogOptionsResult {
    data class Success(val value: CatalogOptionsResponse) : CatalogOptionsResult
    data class UpstreamFailure(val response: UpstreamResponse) : CatalogOptionsResult
    data object Unauthenticated : CatalogOptionsResult
    data class Invalid(val reason: InvalidReason) : CatalogOptionsResult
}

internal enum class InvalidReason { MISSING_ASSIGNMENT, INVALID_JSON }

internal class CatalogOptionsApplicationService(
    private val dependencies: UpstreamRouteDependencies
) {
    suspend fun fetch(category: String, session: UpstreamSession): CatalogOptionsResult {
        val response = dependencies.upstream.request(
            UpstreamRequests.get(
                UpstreamPaths.latestPage(dependencies.upstreamEndpoints),
                session,
                UpstreamHeaders.of(
                    UpstreamHeader(HttpHeaders.Referrer, dependencies.upstreamEndpoints.latestPage),
                    UpstreamHeader(HttpHeaders.Accept, UpstreamProtocolConfig.ACCEPT_HTML_DOCUMENT)
                ),
            policy = UpstreamEndpointPolicy.CATALOG
            )
        )
        if (!response.status.isSuccessful) return CatalogOptionsResult.UpstreamFailure(response)
        if (!isAuthenticatedUpstreamResponse(response, dependencies.upstreamEndpoints.origin)) {
            return CatalogOptionsResult.Unauthenticated
        }

        val rawConfig = extractJavaScriptObject(response.body, "latestUpdates")
        val configResult = rawConfig?.let { decodeJsonResult<LatestUpdatesConfig>(it) }
        val config = (configResult as? ParseResult.Success)?.value
        if (config == null) {
            val reason = if (rawConfig == null) InvalidReason.MISSING_ASSIGNMENT else InvalidReason.INVALID_JSON
            logger.warn { "Unable to parse F95zone catalog options: reason=$reason; upstreamBodyLength=${response.body.length}" }
            return CatalogOptionsResult.Invalid(reason)
        }
        return CatalogOptionsResult.Success(
            CatalogOptionsResponse(config.tags, config.prefixes[category] ?: JsonArray(emptyList()))
        )
    }
}

