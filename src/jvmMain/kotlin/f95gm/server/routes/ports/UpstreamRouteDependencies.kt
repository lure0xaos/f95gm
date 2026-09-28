package f95gm.server.routes.ports

import f95gm.server.config.upstream.UpstreamEndpointConfig
import f95gm.server.upstream.request.UpstreamRequester
import f95gm.server.routes.ports.UpstreamLoginClient

internal interface UpstreamRouteDependencies : ClockRouteDependencies {
    val upstreamEndpoints: UpstreamEndpointConfig
    val upstream: UpstreamRequester
    val upstreamLoginClient: UpstreamLoginClient
}
