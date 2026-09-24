package f95gm.server.state.runtime

import f95gm.server.upstream.request.UpstreamGateway

internal interface UpstreamRouteDependencies {
    val clock: ServerClock
    val upstream: UpstreamGateway
}
