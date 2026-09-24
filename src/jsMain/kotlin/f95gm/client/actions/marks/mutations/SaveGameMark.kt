package f95gm.client.actions.marks.mutations

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.putJson
import f95gm.client.data.marks.parseMarks
import f95gm.client.model.marks.GameMarkDraft
import f95gm.client.model.marks.MarkRequestBody
import f95gm.client.state.marks.marks
import f95gm.client.state.options.localizedTrackingState
import f95gm.client.state.runtime.clientJson
import f95gm.client.state.runtime.scope
import f95gm.domain.api.F95Api
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun saveGameMark(draft: GameMarkDraft) {
    scope.launch {
        val existing = marks.data.first().items.firstOrNull { it.threadId == draft.threadId }
        val storedVersion = if (draft.acknowledge) draft.storedVersion
        else existing?.storedVersion ?: draft.storedVersion
        apiResult {
            putJson(
                F95Api.markPath(draft.threadId),
                clientJson.encodeToString(
                    MarkRequestBody(
                        trackingState = draft.trackingState,
                        storedVersion = storedVersion,
                        acknowledge = draft.acknowledge
                    )
                )
            )
        }.onSuccess { payload ->
            val saved = parseMarks(payload).firstOrNull() ?: return@onSuccess
            marks.enqueue { state ->
                state.copy(items = listOf(saved) + state.items.filterNot { it.threadId == draft.threadId })
            }
            showToast(
                UiMessages.app_trackingStateSaved(
                    localizedTrackingState(
                        draft.trackingState
                    )
                )
            )
        }.onFailure { error ->
            showToast(error.apiUserMessage(UiMessages.app_trackingStateSaveFailed()))
        }
    }
}
