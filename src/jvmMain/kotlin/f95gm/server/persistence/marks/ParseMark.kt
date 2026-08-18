package f95gm.server.persistence.marks

import f95gm.domain.values.F95ThreadId
import f95gm.domain.values.StoredVersion
import f95gm.server.config.filters.gameTrackingStates
import f95gm.server.model.marks.GameMark

internal fun parseMark(threadId: String, value: GameMark): GameMark? {
    val trackingState = value.trackingState.takeIf { it.wireValue in gameTrackingStates } ?: return null
    return value.copy(
        threadId = value.threadId.takeIf { it.value.isNotBlank() } ?: F95ThreadId(threadId),
        trackingState = trackingState,
        storedVersion = value.storedVersion.takeIf { it.value.isNotBlank() }
            ?: StoredVersion("Unknown")
    )
}
