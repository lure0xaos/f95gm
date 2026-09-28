package f95gm.server.routes.marks.live

import f95gm.domain.values.F95ThreadId
import f95gm.server.http.response.isSuccessful
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.ParseResult
import f95gm.server.routes.marks.live.MarkRouteDependencies
import f95gm.server.config.protocol.UpstreamProtocolConfig
import f95gm.server.upstream.request.*
import io.ktor.http.*

internal suspend fun fetchThreadDetailResponse(
    context: MarkRouteDependencies,
    threadId: F95ThreadId,
    session: UpstreamSession
): ParseResult<String> {
    val headers = UpstreamHeaders.of(
        UpstreamHeader(HttpHeaders.Referrer, context.upstreamEndpoints.latestPage),
        UpstreamHeader(HttpHeaders.Accept, UpstreamProtocolConfig.ACCEPT_HTML_DOCUMENT)
    )
    val first = context.upstream.request(
        UpstreamRequests.get(UpstreamPaths.thread(threadId), session, headers, UpstreamEndpointPolicy.DETAIL)
    )
    val result = followUpstreamRedirects(
        context.upstream,
        first,
        session,
        headers,
        UpstreamEndpointPolicy.DETAIL
    )
    return if (result.status.isSuccessful) ParseResult.Success(result.body)
    else ParseResult.Invalid("Upstream detail request failed: ${result.status}")
}
