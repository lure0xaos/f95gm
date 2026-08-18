package f95gm.server.model.marks

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.StoredVersion
import kotlinx.serialization.Serializable

@Serializable
internal data class MarkRequest(
    val trackingState: GameTrackingState? = null,
    val storedVersion: StoredVersion = StoredVersion(F95Defaults.UNKNOWN_VERSION),
    val acknowledge: Boolean = false
)
