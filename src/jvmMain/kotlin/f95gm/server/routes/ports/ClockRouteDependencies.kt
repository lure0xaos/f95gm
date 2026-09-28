package f95gm.server.routes.ports

import f95gm.server.kernel.ServerClock

internal interface ClockRouteDependencies {
    val clock: ServerClock
}
