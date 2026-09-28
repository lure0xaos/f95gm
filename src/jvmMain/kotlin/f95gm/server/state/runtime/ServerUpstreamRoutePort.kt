package f95gm.server.state.runtime

import f95gm.server.config.upstream.UpstreamEndpointConfig
import f95gm.server.kernel.ServerClock
import f95gm.server.routes.ports.ClockRouteDependencies
import f95gm.server.routes.ports.UpstreamLoginClient
import f95gm.server.routes.ports.UpstreamRouteDependencies
import f95gm.server.upstream.request.UpstreamRequester

/** Small adapter exposing only upstream capabilities to route features. */
internal class ServerUpstreamRoutePort(
    override val clock: ServerClock,
    override val upstreamEndpoints: UpstreamEndpointConfig,
    override val upstream: UpstreamRequester,
    override val upstreamLoginClient: UpstreamLoginClient
) : UpstreamRouteDependencies, ClockRouteDependencies
