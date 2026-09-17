package f95gm.server.routes.session

import f95gm.server.application.SessionRegistry
import f95gm.server.application.SessionService
import f95gm.server.routes.ports.UpstreamRouteDependencies

internal interface SessionRouteDependencies : UpstreamRouteDependencies {
    val sessions: SessionRegistry
    val sessionService: SessionService
}
