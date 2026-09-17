package f95gm.client.state.runtime

internal enum class ClientRuntimePhase { CREATED, STARTED, CLOSED }

/** Closing stores and an owned HTTP client is terminal; restarting needs a fresh runtime. */
internal class ClientRuntimeLifecycle {
    var phase: ClientRuntimePhase = ClientRuntimePhase.CREATED
        private set

    fun start() {
        check(phase != ClientRuntimePhase.CLOSED) { "Client runtime is closed; create a new runtime" }
        phase = ClientRuntimePhase.STARTED
    }

    fun close(): Boolean {
        if (phase == ClientRuntimePhase.CLOSED) return false
        phase = ClientRuntimePhase.CLOSED
        return true
    }
}
