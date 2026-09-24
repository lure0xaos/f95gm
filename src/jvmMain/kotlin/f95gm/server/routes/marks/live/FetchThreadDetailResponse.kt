package f95gm.server.routes.marks.live

import f95gm.domain.values.F95ThreadId
import f95gm.server.config.upstream.latestPage
import f95gm.server.http.response.isRedirect
import f95gm.server.http.response.isSuccessful
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.state.runtime.RouteDependencies
import f95gm.server.upstream.request.UpstreamHeaders
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.http.*

internal suspend fun fetchThreadDetailResponse(
    context: RouteDependencies,
    threadId: F95ThreadId,
    session: UpstreamSession
): String? {
    val headers = UpstreamHeaders.of(
        mapOf(
        HttpHeaders.Referrer to latestPage,
        HttpHeaders.Accept to "text/html,application/xhtml+xml"
        )
    )
    val first = context.upstreamRequest(HttpMethod.Get, "/threads/${threadId.value}/", session, headers = headers)
    val location = first.location
    val result =
        if (first.status.isRedirect && location != null) {
            context.upstreamRequest(HttpMethod.Get, location, session, headers = headers)
        } else {
            first
        }
    return result.body.takeIf { result.status.isSuccessful }
}
