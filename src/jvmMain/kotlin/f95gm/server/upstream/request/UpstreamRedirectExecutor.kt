package f95gm.server.upstream.request

import f95gm.server.config.protocol.UpstreamProtocolConfig
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.http.response.isRedirect

/** Executes already-validated redirects; URL policy remains a pure concern of UpstreamUrlPolicy. */
internal suspend fun followUpstreamRedirects(
    gateway: UpstreamRequester,
    initial: UpstreamResponse,
    session: UpstreamSession,
    headers: UpstreamHeaders = UpstreamHeaders.EMPTY,
    policy: UpstreamPolicySelection = UpstreamEndpointPolicy.DEFAULT
): UpstreamResponse {
    var response = initial
    repeat(UpstreamProtocolConfig.MAX_REDIRECTS) {
        if (!response.status.isRedirect) return response
        val location = response.location?.value ?: return response
        when (val safeLocation = validateUpstreamRedirect(location, gateway.endpointConfig.baseUrl)) {
            is UpstreamRedirectResult.Rejected -> return response
            is UpstreamRedirectResult.Accepted -> {
                response = gateway.request(UpstreamRequests.get(safeLocation.path, session, headers, policy))
            }
        }
    }
    return response
}
