package f95gm.client.state.runtime

import f95gm.client.model.runtime.JvmConnectionStatus
import f95gm.client.model.runtime.UpstreamConnectionStatus
import f95gm.client.model.runtime.ApiRequestPath
import f95gm.client.model.runtime.HttpStatusCodeValue

internal data class JvmConnectionState(
    val status: JvmConnectionStatus = JvmConnectionStatus.CONNECTED,
    val healthStatus: HttpStatusCodeValue? = HttpStatusCodeValue(200),
    val healthError: String = "",
    val upstreamStatus: UpstreamConnectionStatus = UpstreamConnectionStatus.UNKNOWN,
    val upstreamHttpStatus: HttpStatusCodeValue? = null,
    val upstreamPath: ApiRequestPath = ApiRequestPath("")
) {
    val connected: Boolean
        get() = status == JvmConnectionStatus.CONNECTED
}
