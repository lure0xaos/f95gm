package f95gm.client.model.marks

import f95gm.domain.values.StoredVersion

internal data class GameMarkDraft(
    val threadId: String,
    val storedVersion: StoredVersion,
    val trackingState: String,
    val acknowledge: Boolean = false
)
