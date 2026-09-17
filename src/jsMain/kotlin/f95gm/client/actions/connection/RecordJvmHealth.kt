package f95gm.client.actions.connection

import f95gm.client.model.runtime.JvmConnectionStatus
import f95gm.client.model.runtime.HttpStatusCodeValue
import f95gm.client.state.runtime.ClientApiEventPort
import f95gm.domain.api.F95HttpStatus
import kotlinx.coroutines.launch

internal fun recordJvmHealth(runtime: ClientApiEventPort, status: Int) {
    val connectionStatus = when {
        status in F95HttpStatus.SUCCESS_MIN..F95HttpStatus.SUCCESS_MAX -> JvmConnectionStatus.CONNECTED
        else -> JvmConnectionStatus.UNEXPECTED_RESPONSE
    }
    runtime.launchEffect {
        runtime.jvmConnection.enqueue {
            it.copy(
                status = connectionStatus,
                healthStatus = HttpStatusCodeValue(status),
                healthError = if (connectionStatus == JvmConnectionStatus.CONNECTED) "" else "HTTP $status"
            )
        }
    }
}
