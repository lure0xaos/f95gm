package f95gm.client.state.runtime

import f95gm.client.model.runtime.JvmConnectionStatus
import f95gm.client.model.runtime.UpstreamConnectionStatus

internal data class JvmConnectionState(
    val status: JvmConnectionStatus = JvmConnectionStatus.CONNECTED,
    val healthStatus: Int? = 200,
    val healthError: String = "",
    val upstreamStatus: UpstreamConnectionStatus = UpstreamConnectionStatus.UNKNOWN,
    val upstreamHttpStatus: Int? = null,
    val upstreamPath: String = ""
) {
    val connected: Boolean
        get() = status == JvmConnectionStatus.CONNECTED
}
