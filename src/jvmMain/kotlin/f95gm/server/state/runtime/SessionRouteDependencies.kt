package f95gm.server.state.runtime

import f95gm.server.state.runtime.session.SessionRegistry
import f95gm.server.state.runtime.session.SessionService

internal interface SessionRouteDependencies {
    val sessions: SessionRegistry
    val sessionService: SessionService
}
