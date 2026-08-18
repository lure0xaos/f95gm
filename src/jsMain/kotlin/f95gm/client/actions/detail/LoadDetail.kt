package f95gm.client.actions.detail

import f95gm.client.data.api.getJson
import f95gm.client.data.detail.parseDetail
import f95gm.client.model.detail.DetailState
import f95gm.client.state.detail.detail
import f95gm.domain.api.F95Api
import f95gm.domain.values.F95ThreadId
import f95gm.messages.UiMessages

internal suspend fun loadDetail(threadId: String) {
    detail.enqueue { DetailState(threadId = F95ThreadId(threadId), loading = true) }
    runCatching {
        parseDetail(getJson("${F95Api.ITEM}/$threadId"))
    }.onSuccess { result ->
        detail.enqueue { result.copy(loading = false, error = "") }
    }.onFailure { error ->
        detail.enqueue {
            DetailState(
                threadId = F95ThreadId(threadId),
                error = error.message ?: UiMessages.app_itemDetailsLoadFailed()
            )
        }
    }
}
