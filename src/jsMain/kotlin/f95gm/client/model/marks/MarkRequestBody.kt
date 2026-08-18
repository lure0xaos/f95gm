package f95gm.client.model.marks

import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.StoredVersion
import kotlinx.serialization.Serializable

@Serializable
internal data class MarkRequestBody(
    val trackingState: GameTrackingState,
    val storedVersion: StoredVersion,
    val acknowledge: Boolean
)
