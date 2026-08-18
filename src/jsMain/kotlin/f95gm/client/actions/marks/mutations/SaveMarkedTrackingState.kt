package f95gm.client.actions.marks.mutations

import f95gm.client.model.marks.GameMarkDraft
import f95gm.client.model.marks.MarkedGame
import f95gm.domain.values.StoredVersion

internal fun saveMarkedTrackingState(mark: MarkedGame, trackingState: String, acknowledge: Boolean = false) {
    saveGameMark(
        GameMarkDraft(
            threadId = mark.threadId.value,
            storedVersion = StoredVersion(if (acknowledge) mark.latestVersion.value else mark.version.value),
            trackingState = trackingState,
            acknowledge = acknowledge
        )
    )
}
