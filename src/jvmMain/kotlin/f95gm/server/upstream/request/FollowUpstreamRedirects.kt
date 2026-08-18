package f95gm.server.upstream.request

import f95gm.server.config.constants.ServerConstants
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse

internal suspend fun followUpstreamRedirects(
    initial: UpstreamResponse,
    session: UpstreamSession
): UpstreamResponse {
    var response = initial
    repeat(ServerConstants.MAX_UPSTREAM_REDIRECTS) {
        if (response.status !in ServerConstants.REDIRECT_STATUS_MIN..ServerConstants.REDIRECT_STATUS_MAX) return response
        val location = response.location?.takeIf { it.isNotBlank() } ?: return response
        response = upstreamRequest("GET", location, session)
    }
    return response
}
