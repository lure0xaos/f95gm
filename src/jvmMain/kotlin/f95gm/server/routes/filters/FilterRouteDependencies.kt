package f95gm.server.routes.filters

import f95gm.server.routes.ports.ApplicationRouteDependencies
import f95gm.server.routes.ports.ClockRouteDependencies
import f95gm.server.routes.session.SessionRouteDependencies

internal interface FilterRouteDependencies : SessionRouteDependencies, ApplicationRouteDependencies, ClockRouteDependencies
