package f95gm.client.state.runtime

import f95gm.client.data.api.ApiFailure
import f95gm.client.data.api.ApiResult
import f95gm.client.data.api.apiResult

/** Shared operation wrapper for actions that expose a typed API result. */
internal fun <T> ClientOperationPort.launchApiOperation(
    key: ClientJobKey,
    request: suspend () -> T,
    onSuccess: suspend (T) -> Unit = {},
    onFailure: suspend (ApiFailure) -> Unit = {}
) = launchOperation(key) {
    when (val result = apiResult(request)) {
        is ApiResult.Success -> onSuccess(result.value)
        is ApiResult.Failure -> onFailure(result.failure)
    }
}
