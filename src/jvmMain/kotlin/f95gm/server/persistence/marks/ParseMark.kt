package f95gm.server.persistence.marks

import f95gm.domain.values.F95ThreadId
import f95gm.domain.values.StoredVersion
import f95gm.server.config.filters.gameTrackingStates
import f95gm.server.model.marks.GameMark
import f95gm.server.model.ParseResult

internal fun parseMarkResult(threadId: String, value: GameMark): ParseResult<GameMark> {
    val trackingState = value.trackingState.takeIf { it.wireValue in gameTrackingStates }
        ?: return ParseResult.Invalid("Unknown tracking state", f95gm.server.model.ParseFailureCategory.INVALID_TRACKING_STATE)
    return ParseResult.Success(value.copy(
        threadId = value.threadId.takeIf { it.value.isNotBlank() } ?: F95ThreadId(threadId),
        trackingState = trackingState,
        storedVersion = value.storedVersion.takeIf { it.value.isNotBlank() }
            ?: StoredVersion("Unknown")
    ))
}
