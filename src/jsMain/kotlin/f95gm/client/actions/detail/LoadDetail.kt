package f95gm.client.actions.detail

import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.ApiFallbacks
import f95gm.client.model.detail.DetailState
import f95gm.client.state.runtime.ClientDetailActionsPort
import f95gm.client.state.runtime.ClientJobKey
import f95gm.domain.values.F95ThreadId
import f95gm.messages.UiMessages
import f95gm.client.state.runtime.AsyncState

internal suspend fun loadDetail(runtime: ClientDetailActionsPort, threadId: F95ThreadId) {
    val requestToken = runtime.requests.beginRequest(ClientJobKey.DETAIL_LOAD)
    runtime.detail.enqueue { DetailState(threadId = threadId, async = AsyncState.Loading) }
    apiResult {
        runtime.detailApi.detail(threadId)
    }.onSuccess { result ->
        if (!runtime.requests.isCurrentRequest(ClientJobKey.DETAIL_LOAD, requestToken)) return@onSuccess
        runtime.detail.enqueue { result.copy(async = AsyncState.Success(Unit)) }
    }.onFailure { error ->
        if (!runtime.requests.isCurrentRequest(ClientJobKey.DETAIL_LOAD, requestToken)) return@onFailure
        runtime.detail.enqueue {
            DetailState(
                threadId = threadId,
                async = AsyncState.Failure(error.apiUserMessage(ApiFallbacks.ItemDetails))
            )
        }
    }
}
