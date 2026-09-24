package f95gm.client.actions.marks.mutations

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.deleteJson
import f95gm.client.model.marks.markedGamesView
import f95gm.client.state.marks.marks
import f95gm.client.state.runtime.scope
import f95gm.domain.api.F95Api
import f95gm.domain.values.F95ThreadId
import f95gm.messages.UiMessages
import kotlinx.coroutines.launch

internal fun unmarkGame(threadId: F95ThreadId) {
    scope.launch {
        apiResult { deleteJson(F95Api.markPath(threadId)) }
            .onSuccess {
                marks.enqueue { state ->
                    val next = state.copy(items = state.items.filterNot { it.threadId == threadId })
                    next.copy(page = markedGamesView(next).page)
                }
                showToast(UiMessages.app_gameRemoved())
            }
            .onFailure { error -> showToast(error.apiUserMessage(UiMessages.app_trackingStateRemoveFailed())) }
    }
}
