package f95gm.client.actions.runtime

import f95gm.client.model.runtime.UpstreamConnectionStatus
import f95gm.client.state.runtime.jvmConnection
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun recordApiResponse(path: String, status: Int) {
    val upstreamStatus = when {
        status == 401 -> UpstreamConnectionStatus.AUTHENTICATION_REQUIRED
        status in 200..399 -> UpstreamConnectionStatus.RESPONDING
        status >= 500 -> UpstreamConnectionStatus.UNAVAILABLE
        else -> UpstreamConnectionStatus.ERROR
    }
    scope.launch {
        jvmConnection.enqueue {
            it.copy(
                upstreamStatus = upstreamStatus,
                upstreamHttpStatus = status,
                upstreamPath = path
            )
        }
    }
}
