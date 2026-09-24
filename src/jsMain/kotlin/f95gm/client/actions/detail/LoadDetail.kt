package f95gm.client.actions.detail

import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.getJson
import f95gm.client.data.detail.parseDetail
import f95gm.client.model.detail.DetailState
import f95gm.client.state.detail.detail
import f95gm.domain.api.F95Api
import f95gm.domain.values.F95ThreadId
import f95gm.messages.UiMessages

internal suspend fun loadDetail(threadId: F95ThreadId) {
    detail.enqueue { DetailState(threadId = threadId, loading = true) }
    apiResult {
        parseDetail(getJson(F95Api.itemPath(threadId)))
    }.onSuccess { result ->
        detail.enqueue { result.copy(loading = false, error = "") }
    }.onFailure { error ->
        detail.enqueue {
            DetailState(
                threadId = threadId,
                error = error.apiUserMessage(UiMessages.app_itemDetailsLoadFailed())
            )
        }
    }
}
