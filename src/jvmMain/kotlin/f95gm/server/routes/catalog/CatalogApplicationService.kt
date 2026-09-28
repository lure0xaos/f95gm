package f95gm.server.routes.catalog

import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.routes.ports.UpstreamRouteDependencies
import f95gm.server.config.upstream.UpstreamEndpointConfig
import f95gm.server.config.protocol.UpstreamProtocolConfig
import f95gm.server.upstream.request.*
import io.ktor.http.*

internal class CatalogApplicationService(private val dependencies: UpstreamRouteDependencies) {
    suspend fun fetch(path: UpstreamPath, session: UpstreamSession): UpstreamResponse =
        dependencies.upstream.request(catalogRequest(path, session, dependencies.upstreamEndpoints))
}

/** Shared request contract for catalog pages and live-mark catalog lookups. */
internal fun catalogRequest(
    path: UpstreamPath,
    session: UpstreamSession,
    endpoints: UpstreamEndpointConfig
) = UpstreamRequests.get(
    path,
    session,
    UpstreamHeaders.of(
        UpstreamHeader(HttpHeaders.Referrer, endpoints.latestPage),
        UpstreamHeader(UpstreamProtocolConfig.X_REQUESTED_WITH, UpstreamProtocolConfig.XML_HTTP_REQUEST),
        UpstreamHeader(HttpHeaders.Accept, UpstreamProtocolConfig.ACCEPT_CATALOG_JSON)
    ),
    policy = UpstreamEndpointPolicy.CATALOG
)
