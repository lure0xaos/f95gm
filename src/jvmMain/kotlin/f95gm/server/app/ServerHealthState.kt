package f95gm.server.app

import f95gm.server.kernel.ValidatedState

internal enum class ServerHealthPhase { STARTING, SERVING, STOPPING, STOPPED, FAILED }

/** Explicit probe state; liveness and readiness are not inferred from route side effects. */
internal class ServerHealthState(initial: ServerHealthPhase = ServerHealthPhase.STARTING) {
    private val phase = ValidatedState(initial, allowedTransitions)

    fun markServing() = transition(ServerHealthPhase.SERVING)
    fun markStopping() = transition(ServerHealthPhase.STOPPING)
    fun markStopped() = transition(ServerHealthPhase.STOPPED)
    fun markFailed() = transition(ServerHealthPhase.FAILED)
    fun isLive(): Boolean = phase.current() !in setOf(ServerHealthPhase.FAILED, ServerHealthPhase.STOPPED)
    fun isReady(): Boolean = phase.current() == ServerHealthPhase.SERVING
    fun current(): ServerHealthPhase = phase.current()

    private fun transition(next: ServerHealthPhase) = phase.transition(next, allowRepeated = true)

    private companion object {
        val allowedTransitions: Map<ServerHealthPhase, Set<ServerHealthPhase>> = mapOf(
            ServerHealthPhase.STARTING to setOf(ServerHealthPhase.SERVING, ServerHealthPhase.STOPPING, ServerHealthPhase.FAILED),
            ServerHealthPhase.SERVING to setOf(ServerHealthPhase.STOPPING, ServerHealthPhase.FAILED),
            ServerHealthPhase.STOPPING to setOf(ServerHealthPhase.STOPPED, ServerHealthPhase.FAILED),
            ServerHealthPhase.STOPPED to emptySet(),
            ServerHealthPhase.FAILED to setOf(ServerHealthPhase.STOPPING)
        )
    }
}
