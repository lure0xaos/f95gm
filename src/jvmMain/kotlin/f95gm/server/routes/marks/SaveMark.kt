package f95gm.server.routes.marks

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.StoredVersion
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.filters.gameTrackingStates
import f95gm.server.http.request.*
import f95gm.server.http.response.*
import f95gm.server.model.EpochMillis
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.MarkRequest
import f95gm.server.persistence.ConditionalWriteResult
import f95gm.server.persistence.marks.marksJson
import f95gm.server.routes.marks.live.invalidateLiveMark
import f95gm.server.routes.marks.live.loadLiveMarks
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.state.runtime.IdempotencyRegistry
import f95gm.server.state.runtime.routeDependencies
import io.ktor.server.application.*

internal suspend fun saveMark(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToMark()) ?: return
    val idempotencyKey = validIdempotencyKey(call.request.headers[ServerConstants.IDEMPOTENCY_KEY_HEADER])
    val mutationKey = idempotencyKey?.value?.let { "mark:${session.accountKey.value}:$it" }
    val threadId = call.positiveThreadIdParameter("threadId")
    if (threadId == null) {
        respondFailure(
            call,
            ServerConstants.STATUS_BAD_REQUEST,
            ApiFailure(
                UiMessages.server_invalidGameId(),
                ServerMessageKeys.INVALID_GAME_ID,
                F95ApiErrorCode.INVALID_INPUT,
                ApiFailureDetails.field("threadId")
            )
        )
        return
    }

    val body = call.receiveLimitedText(ServerConstants.MAX_REQUEST_BODY_BYTES)
    val request = when (val decoded = decodeJson<MarkRequest>(body)) {
        is RequestValidation.Valid -> decoded.value
        is RequestValidation.Invalid -> {
            respondFailure(
                call,
                ServerConstants.STATUS_BAD_REQUEST,
                decoded.toApiFailure()
            )
            return
        }
    }
    val trackingState = request.trackingState?.wireValue?.takeIf { it in gameTrackingStates }
    if (trackingState == null) {
        respondFailure(
            call,
            ServerConstants.STATUS_BAD_REQUEST,
            ApiFailure(
                UiMessages.server_invalidGameTrackingState(),
                ServerMessageKeys.INVALID_TRACKING_STATE,
                F95ApiErrorCode.INVALID_INPUT,
                ApiFailureDetails.field("trackingState")
            )
        )
        return
    }
    val lease = call.beginIdempotentMutation(mutationKey, "$threadId\n$body") ?: return
    lease.use {
        val existing = call.routeDependencies.applicationServices.marks.load(session.accountKey)
            .firstOrNull { it.threadId == threadId }
    val requestedStoredVersion = request.storedVersion.value.trim().take(ServerConstants.MAX_TEXT_LENGTH)
        .ifBlank { existing?.storedVersion?.value ?: F95Defaults.UNKNOWN_VERSION }
    val requestedMark = GameMark(
        threadId = threadId,
        trackingState = GameTrackingState.fromWire(trackingState) ?: return,
        storedVersion = if (request.acknowledge) {
            StoredVersion(requestedStoredVersion)
        } else {
            existing?.storedVersion ?: StoredVersion(requestedStoredVersion)
        },
        lastSeenUpdateVersion = existing?.lastSeenUpdateVersion ?: StoredVersion(F95Defaults.EMPTY),
        updatedAt = EpochMillis(call.routeDependencies.clock.nowMillis())
    )
        val live = loadLiveMarks(call.routeDependencies, listOf(requestedMark), session)
    val effectiveStoredVersion = if (existing == null && !request.acknowledge) {
        live[threadId.value]?.version?.value
            ?.takeIf { it.isNotBlank() && !it.equals(F95Defaults.UNKNOWN_VERSION, ignoreCase = true) }
            ?.let(::StoredVersion)
            ?: requestedMark.storedVersion
    } else {
        requestedMark.storedVersion
    }
    val mark = requestedMark.copy(storedVersion = effectiveStoredVersion)
        if (call.routeDependencies.applicationServices.marks.storeIfCurrent(
                session.accountKey,
                mark,
                request.expectedUpdatedAt?.value
            ) is ConditionalWriteResult.Conflict
        ) {
            it.release()
            respondApiFailure(
                call,
                ServerConstants.STATUS_CONFLICT,
                UiMessages.server_markConflict(),
                ServerMessageKeys.MARK_CONFLICT,
                F95ApiErrorCode.INVALID_INPUT
            )
            return
        }
        invalidateLiveMark(call.routeDependencies, threadId)
        val responseBody = marksJson(listOf(mark), live)
        it.complete(
            IdempotencyRegistry.CachedResponse(
                ServerConstants.STATUS_OK.value,
                responseBody,
                null
            )
        )
        respond(call, ServerConstants.STATUS_OK, responseBody)
    }
}
