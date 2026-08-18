package f95gm.client.actions.runtime

import f95gm.client.model.runtime.JvmConnectionStatus
import f95gm.client.state.runtime.jvmConnection
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun recordJvmUnavailable() {
    scope.launch {
        jvmConnection.enqueue {
            it.copy(
                status = JvmConnectionStatus.UNAVAILABLE,
                healthStatus = null,
                healthError = ""
            )
        }
    }
}
