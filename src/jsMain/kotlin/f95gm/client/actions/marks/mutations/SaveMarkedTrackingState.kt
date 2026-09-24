package f95gm.client.actions.marks.mutations

import f95gm.client.model.marks.GameMarkDraft
import f95gm.client.model.marks.MarkedGame
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.StoredVersion

internal fun saveMarkedTrackingState(mark: MarkedGame, trackingState: GameTrackingState, acknowledge: Boolean = false) {
    saveGameMark(
        GameMarkDraft(
            threadId = mark.threadId,
            storedVersion = StoredVersion(if (acknowledge) mark.latestVersion.value else mark.version.value),
            trackingState = trackingState,
            acknowledge = acknowledge
        )
    )
}
