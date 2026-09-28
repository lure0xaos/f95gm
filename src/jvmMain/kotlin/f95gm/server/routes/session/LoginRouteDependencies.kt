package f95gm.server.routes.session

import f95gm.server.config.Bytes
import f95gm.server.application.SessionService
import f95gm.server.config.upstream.UpstreamEndpointConfig
import f95gm.server.routes.ports.UpstreamLoginClient
import f95gm.server.upstream.request.UpstreamRequester

internal interface LoginApplicationDependencies {
    val sessionService: SessionService
    val upstreamEndpoints: UpstreamEndpointConfig
    val upstream: UpstreamRequester
    val upstreamLoginClient: UpstreamLoginClient
}

internal interface LoginRouteDependencies : LoginApplicationDependencies {
    val loginService: LoginApplicationService
    val maxRequestBodyBytes: Bytes
}
