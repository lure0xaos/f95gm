package f95gm.client.actions.marks.mutations

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.ApiFallbacks
import f95gm.client.model.marks.markedGamesView
import f95gm.client.state.runtime.ClientMarksActionsPort
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.launchApiOperation
import f95gm.domain.values.F95ThreadId
import f95gm.messages.UiMessages

internal fun unmarkGame(runtime: ClientMarksActionsPort, threadId: F95ThreadId) {
    runtime.launchApiOperation(
        key = ClientJobKey.markMutation(threadId),
        request = { runtime.marksApi.deleteMark(threadId) },
        onSuccess = {
                runtime.marks.enqueue { state ->
                    val next = state.copy(items = state.items.filterNot { it.threadId == threadId })
                    next.copy(page = markedGamesView(next).page)
                }
                showToast(runtime, UiMessages.app_gameRemoved())
        },
        onFailure = { error -> showToast(runtime, error.apiUserMessage(ApiFallbacks.TrackingRemove)) }
    )
}
