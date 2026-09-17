package f95gm.domain.api

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95EpochMillis
import f95gm.domain.values.StoredVersion
import kotlinx.serialization.Serializable

@Serializable
data class F95MarkRequest(
    val trackingState: GameTrackingState? = null,
    val storedVersion: StoredVersion = StoredVersion(F95Defaults.UNKNOWN_VERSION),
    val acknowledge: Boolean = false,
    val expectedUpdatedAt: F95EpochMillis? = null
)
