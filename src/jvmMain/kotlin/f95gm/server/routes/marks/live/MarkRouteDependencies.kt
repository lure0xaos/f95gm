package f95gm.server.routes.marks.live

import f95gm.server.routes.ports.ApplicationRouteDependencies
import f95gm.server.routes.ports.LiveMarkRouteDependencies
import f95gm.server.routes.ports.UpstreamRouteDependencies
import f95gm.server.routes.session.SessionRouteDependencies

internal interface MarkRouteDependencies :
    SessionRouteDependencies,
    ApplicationRouteDependencies,
    LiveMarkRouteDependencies,
    UpstreamRouteDependencies
