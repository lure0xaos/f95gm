package f95gm.server.routes.marks.live

import f95gm.domain.api.F95HttpStatus
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.upstream.latestPage
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.http.*

internal suspend fun fetchThreadDetailResponse(threadId: String, session: UpstreamSession): String? {
    val headers = mapOf(
        HttpHeaders.Referrer to latestPage,
        HttpHeaders.Accept to "text/html,application/xhtml+xml"
    )
    val first = upstreamRequest("GET", "/threads/$threadId/", session, headers = headers)
    val result =
        if (first.status in ServerConstants.REDIRECT_STATUS_MIN..ServerConstants.REDIRECT_STATUS_MAX && first.location != null) {
            upstreamRequest("GET", first.location, session, headers = headers)
        } else {
            first
        }
    return result.body.takeIf { result.status in F95HttpStatus.SUCCESS_MIN..F95HttpStatus.SUCCESS_MAX }
}
