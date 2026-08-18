package f95gm.server.routes.marks

import f95gm.domain.api.F95HttpStatus
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95ThreadId
import f95gm.domain.values.StoredVersion
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.filters.gameTrackingStates
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.MarkRequest
import f95gm.server.persistence.marks.loadMarks
import f95gm.server.persistence.marks.marksJson
import f95gm.server.persistence.marks.storeMark
import f95gm.server.routes.marks.live.loadLiveMarks
import f95gm.server.routes.session.browserSession
import f95gm.server.state.runtime.upstreamJson
import io.ktor.server.application.*
import io.ktor.server.request.*

internal suspend fun saveMark(call: ApplicationCall) {
    val session = browserSession(call)?.second
    if (session == null) {
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(UiMessages.server_signInToMark()))
        return
    }
    val threadId = call.parameters["threadId"]?.toLongOrNull()?.takeIf { it > 0 }?.toString()
    if (threadId == null) {
        respond(call, ServerConstants.STATUS_BAD_REQUEST, jsonMessage(UiMessages.server_invalidGameId()))
        return
    }

    val body = call.receiveText()
    val request = runCatching { upstreamJson.decodeFromString<MarkRequest>(body) }.getOrNull()
    val trackingState = request?.trackingState?.wireValue?.takeIf { it in gameTrackingStates }
    if (trackingState == null) {
        respond(call, ServerConstants.STATUS_BAD_REQUEST, jsonMessage(UiMessages.server_invalidGameTrackingState()))
        return
    }
    val existing = loadMarks(session.accountKey).firstOrNull { it.threadId.value == threadId }
    val requestedStoredVersion = request.storedVersion.value.trim().take(ServerConstants.MAX_TEXT_LENGTH)
        .ifBlank { existing?.storedVersion?.value ?: F95Defaults.UNKNOWN_VERSION }
    val requestedMark = GameMark(
        threadId = F95ThreadId(threadId),
        trackingState = GameTrackingState.fromWire(trackingState) ?: return,
        storedVersion = if (request.acknowledge) {
            StoredVersion(requestedStoredVersion)
        } else {
            existing?.storedVersion ?: StoredVersion(requestedStoredVersion)
        },
        lastSeenUpdateVersion = existing?.lastSeenUpdateVersion ?: StoredVersion(F95Defaults.EMPTY),
        updatedAt = System.currentTimeMillis()
    )
    val live = loadLiveMarks(listOf(requestedMark), session)
    val effectiveStoredVersion = if (existing == null && !request.acknowledge) {
        live[threadId]?.version?.value
            ?.takeIf { it.isNotBlank() && !it.equals(F95Defaults.UNKNOWN_VERSION, ignoreCase = true) }
            ?.let(::StoredVersion)
            ?: requestedMark.storedVersion
    } else {
        requestedMark.storedVersion
    }
    val mark = requestedMark.copy(storedVersion = effectiveStoredVersion)
    storeMark(session.accountKey, mark)
    respond(call, F95HttpStatus.SUCCESS_MIN, marksJson(listOf(mark), live))
}
