package f95gm.client.actions.marks.mutations

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.ApiFallbacks
import f95gm.client.model.marks.GameMarkDraft
import f95gm.domain.api.F95MarkRequest
import f95gm.client.state.options.localizedTrackingState
import f95gm.client.state.runtime.ClientMarksActionsPort
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.launchClientOperation
import f95gm.client.state.runtime.launchMarkMutationOperation
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.first

internal fun saveGameMark(runtime: ClientMarksActionsPort, draft: GameMarkDraft) {
    runtime.launchMarkMutationOperation(draft.threadId) {
        val existing = runtime.marks.data.first().items.firstOrNull { it.threadId == draft.threadId }
        val storedVersion = if (draft.acknowledge) draft.storedVersion
        else existing?.storedVersion ?: draft.storedVersion
        apiResult {
            runtime.marksApi.saveMark(
                draft.threadId,
                F95MarkRequest(
                    trackingState = draft.trackingState,
                    storedVersion = storedVersion,
                    acknowledge = draft.acknowledge
                )
            )
        }.onSuccess { saved ->
            runtime.marks.enqueue { state ->
                state.copy(items = listOf(saved) + state.items.filterNot { it.threadId == draft.threadId })
            }
            showToast(runtime,
                UiMessages.app_trackingStateSaved(
                    localizedTrackingState(
                        draft.trackingState
                    )
                )
            )
        }.onFailure { error ->
            showToast(runtime, error.apiUserMessage(ApiFallbacks.TrackingSave))
        }
    }
}
