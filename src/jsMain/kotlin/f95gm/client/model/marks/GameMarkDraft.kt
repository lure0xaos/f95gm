package f95gm.client.model.marks

import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95ThreadId
import f95gm.domain.values.StoredVersion

internal data class GameMarkDraft(
    val threadId: F95ThreadId,
    val storedVersion: StoredVersion,
    val trackingState: GameTrackingState,
    val acknowledge: Boolean = false
)
