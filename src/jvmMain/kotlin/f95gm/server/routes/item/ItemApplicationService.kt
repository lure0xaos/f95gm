package f95gm.server.routes.item

import f95gm.server.routes.ports.UpstreamRouteDependencies
import f95gm.server.config.protocol.UpstreamProtocolConfig
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.domain.values.F95ThreadId
import f95gm.server.upstream.request.*
import io.ktor.http.*

internal class ItemApplicationService(private val dependencies: UpstreamRouteDependencies) {
    suspend fun fetch(threadId: F95ThreadId, session: UpstreamSession): UpstreamResponse {
        val headers = UpstreamHeaders.of(
            UpstreamHeader(HttpHeaders.Referrer, dependencies.upstreamEndpoints.latestPage),
            UpstreamHeader(HttpHeaders.Accept, UpstreamProtocolConfig.ACCEPT_HTML_DOCUMENT)
        )
        val first = dependencies.upstream.request(
            UpstreamRequests.get(UpstreamPaths.thread(threadId), session, headers, UpstreamEndpointPolicy.DETAIL)
        )
        return followUpstreamRedirects(
            dependencies.upstream,
            first,
            session,
            headers,
            UpstreamEndpointPolicy.DETAIL
        )
    }
}
