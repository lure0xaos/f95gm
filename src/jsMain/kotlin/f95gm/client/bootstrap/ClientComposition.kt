package f95gm.client.bootstrap

import f95gm.client.state.runtime.ClientRuntime
import f95gm.client.state.runtime.ClientRuntimeDependencies
import f95gm.client.state.runtime.ClientState

/** Composition root for the browser client; feature code receives the assembled runtime. */
internal object ClientComposition {
    fun create(state: ClientState): ClientRuntime = ClientRuntime(
        state,
        dependencies = ClientRuntimeDependencies.production()
    )
}
