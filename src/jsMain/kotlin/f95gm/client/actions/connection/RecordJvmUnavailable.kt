package f95gm.client.actions.connection

import f95gm.client.model.runtime.JvmConnectionStatus
import f95gm.client.state.runtime.ClientApiEventPort
import kotlinx.coroutines.launch

internal fun recordJvmUnavailable(runtime: ClientApiEventPort) {
    runtime.launchEffect {
        runtime.jvmConnection.enqueue {
            it.copy(
                status = JvmConnectionStatus.UNAVAILABLE,
                healthStatus = null,
                healthError = ""
            )
        }
    }
}
