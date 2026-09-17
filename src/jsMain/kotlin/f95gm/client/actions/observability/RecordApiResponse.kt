package f95gm.client.actions.observability

import f95gm.client.model.runtime.UpstreamConnectionStatus
import f95gm.client.model.runtime.ApiRequestPath
import f95gm.client.model.runtime.HttpStatusCodeValue
import f95gm.client.model.runtime.HttpStatusClass
import f95gm.domain.api.ApiPath
import f95gm.client.state.runtime.ClientApiEventPort
import kotlinx.coroutines.launch

internal fun recordApiResponse(runtime: ClientApiEventPort, path: ApiPath, status: HttpStatusCodeValue) {
    val upstreamStatus = when (HttpStatusClass.from(status)) {
        HttpStatusClass.AUTHENTICATION_REQUIRED -> UpstreamConnectionStatus.AUTHENTICATION_REQUIRED
        HttpStatusClass.RESPONDING -> UpstreamConnectionStatus.RESPONDING
        HttpStatusClass.UNAVAILABLE -> UpstreamConnectionStatus.UNAVAILABLE
        HttpStatusClass.ERROR -> UpstreamConnectionStatus.ERROR
    }
    runtime.launchEffect {
        runtime.jvmConnection.enqueue {
            it.copy(
                upstreamStatus = upstreamStatus,
                upstreamHttpStatus = status,
                upstreamPath = ApiRequestPath(path.value)
            )
        }
    }
}
