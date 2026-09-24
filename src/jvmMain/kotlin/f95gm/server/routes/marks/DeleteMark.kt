package f95gm.server.routes.marks

import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.request.beginIdempotentMutation
import f95gm.server.http.request.positiveThreadIdParameter
import f95gm.server.http.request.validIdempotencyKey
import f95gm.server.http.response.*
import f95gm.server.model.http.OkResponse
import f95gm.server.routes.marks.live.invalidateLiveMark
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.state.runtime.IdempotencyRegistry
import f95gm.server.state.runtime.routeDependencies
import io.ktor.server.application.*

internal suspend fun deleteMark(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToUnmark()) ?: return
    val threadId = call.positiveThreadIdParameter("threadId")
    if (threadId == null) {
        respondFailure(
            call,
            ServerConstants.STATUS_BAD_REQUEST,
            ApiFailure(UiMessages.server_invalidGameId(), ServerMessageKeys.INVALID_GAME_ID)
        )
        return
    }
    val mutationKey = validIdempotencyKey(call.request.headers[ServerConstants.IDEMPOTENCY_KEY_HEADER])?.value
        ?.let { "delete-mark:${session.accountKey.value}:$it:${threadId.value}" }
    val lease = call.beginIdempotentMutation(mutationKey, threadId.value) ?: return
    lease.use {
        call.routeDependencies.applicationServices.marks.remove(session.accountKey, threadId)
        invalidateLiveMark(call.routeDependencies, threadId)
        val responseBody = encodeJson(OkResponse(true))
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
