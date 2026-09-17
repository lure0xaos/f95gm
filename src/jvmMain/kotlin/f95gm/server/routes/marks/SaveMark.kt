package f95gm.server.routes.marks

import f95gm.domain.api.F95ApiErrorCode
import f95gm.messages.UiMessages
import f95gm.server.config.protocol.HttpProtocolConfig
import f95gm.server.http.request.*
import f95gm.server.routes.ports.beginIdempotentMutation
import f95gm.server.http.response.*
import f95gm.server.model.marks.GameMark
import f95gm.server.application.port.CachedResponse
import f95gm.domain.api.F95MarkRequest
import f95gm.server.model.ConditionalWriteResult
import f95gm.server.model.EpochMillis
import f95gm.server.http.response.marksJson
import f95gm.server.routes.marks.live.invalidateLiveMark
import f95gm.server.routes.marks.live.loadLiveMarks
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.application.IdempotencyRegistry
import f95gm.server.routes.marks.markDependencies
import f95gm.server.routes.marks.live.MarkRouteDependencies
import io.ktor.server.application.*
import f95gm.domain.api.ApiStatusCode
import f95gm.server.config.constants.HttpStatusCatalog

internal suspend fun saveMark(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToMark()) ?: return
    val dependencies = call.markDependencies
    val idempotencyKey = validIdempotencyKey(call.request.headers[HttpProtocolConfig.IDEMPOTENCY_KEY_HEADER])
    val mutationKey = idempotencyKey?.value?.let { "mark:${session.accountKey.value}:$it" }
    val threadId = call.positiveThreadIdParameter("threadId")
    if (threadId == null) {
        respondFailure(
            call,
            badRequestResponse(ApiFailure(
                UiMessages.server_invalidGameId(),
                ServerMessageKey.INVALID_GAME_ID,
                F95ApiErrorCode.INVALID_INPUT,
                ApiFailureDetails.field("threadId")
            ))
        )
        return
    }

    val received = call.receiveMutationRequest<F95MarkRequest>(dependencies.maxRequestBodyBytes.value) ?: return
    val body = received.body
    val request = received.request
    if (!request.hasValidTrackingState()) {
        respondFailure(
            call,
            badRequestResponse(ApiFailure(
                UiMessages.server_invalidGameTrackingState(),
                ServerMessageKey.INVALID_TRACKING_STATE,
                F95ApiErrorCode.INVALID_INPUT,
                ApiFailureDetails.field("trackingState")
            ))
        )
        return
    }
    val lease = call.beginIdempotentMutation(mutationKey, "$threadId\n$body") ?: return
    lease.use {
        val existing = dependencies.applicationServices.marks.load(session.accountKey)
            .firstOrNull { it.threadId == threadId }
    val requestedMark = dependencies.applicationServices.marks.createMark(
        threadId, request, existing, dependencies.clock.epochMillis()
    )
        val live = loadLiveMarks(dependencies, listOf(requestedMark), session)
    val effectiveStoredVersion = effectiveStoredVersion(
        existing,
        requestedMark,
        request.acknowledge,
        live[threadId]?.version?.value
    )
    val mark = requestedMark.copy(storedVersion = effectiveStoredVersion)
        val writeResult = dependencies.applicationServices.marks.storeAtomically(
                session.accountKey,
                mark,
                request.expectedUpdatedAt?.value?.let(::EpochMillis)
            )
        if (writeResult is ConditionalWriteResult.Conflict) {
            it.release()
            val failure = conflictResponse(
                MutationResponsePolicy.MARK, writeResult.expectedUpdatedAt, writeResult.actualUpdatedAt
            )
            respondFailure(call, failure)
            return
        }
        invalidateLiveMark(dependencies, threadId)
        val responseBody = marksJson(listOf(mark), live)
        it.complete(
            CachedResponse(
                ApiStatusCode(HttpStatusCatalog.OK.value),
                responseBody,
                null
            )
        )
        respond(call, HttpStatusCatalog.OK, responseBody)
    }
}


