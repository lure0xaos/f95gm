package f95gm.server.model.marks

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95ThreadId
import f95gm.domain.values.StoredVersion
import kotlinx.serialization.Serializable

@Serializable
internal data class GameMark(
    val threadId: F95ThreadId = F95ThreadId(F95Defaults.EMPTY),
    val trackingState: GameTrackingState = GameTrackingState.WATCHING,
    val storedVersion: StoredVersion = StoredVersion(F95Defaults.EMPTY),
    val lastSeenUpdateVersion: StoredVersion = StoredVersion(F95Defaults.EMPTY),
    val updatedAt: Long = 0L
)
