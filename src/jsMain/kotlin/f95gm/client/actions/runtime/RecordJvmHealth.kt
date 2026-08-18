package f95gm.client.actions.runtime

import f95gm.client.model.runtime.JvmConnectionStatus
import f95gm.client.state.runtime.jvmConnection
import f95gm.client.state.runtime.scope
import f95gm.domain.api.F95HttpStatus
import kotlinx.coroutines.launch

internal fun recordJvmHealth(status: Int) {
    val connectionStatus = when {
        status in F95HttpStatus.SUCCESS_MIN..F95HttpStatus.SUCCESS_MAX -> JvmConnectionStatus.CONNECTED
        else -> JvmConnectionStatus.UNEXPECTED_RESPONSE
    }
    scope.launch {
        jvmConnection.enqueue {
            it.copy(
                status = connectionStatus,
                healthStatus = status,
                healthError = if (connectionStatus == JvmConnectionStatus.CONNECTED) "" else "HTTP $status"
            )
        }
    }
}
