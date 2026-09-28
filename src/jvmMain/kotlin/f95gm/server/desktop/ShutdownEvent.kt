package f95gm.server.desktop

internal sealed interface ShutdownEvent {
    data object Request : ShutdownEvent
    data object Completed : ShutdownEvent
    data object Close : ShutdownEvent
}

internal fun transitionShutdownState(
    state: ServerShutdownState,
    event: ShutdownEvent
): ServerShutdownState = when (event) {
    ShutdownEvent.Request -> if (state == ServerShutdownState.RUNNING) ServerShutdownState.DRAINING else state
    ShutdownEvent.Completed, ShutdownEvent.Close -> ServerShutdownState.STOPPED
}
