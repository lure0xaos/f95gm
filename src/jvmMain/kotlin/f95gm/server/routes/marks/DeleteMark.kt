package f95gm.server.routes.marks

import f95gm.messages.UiMessages
import f95gm.server.config.protocol.HttpProtocolConfig
import f95gm.server.routes.ports.beginIdempotentMutation
import f95gm.server.http.request.positiveThreadIdParameter
import f95gm.server.http.request.validIdempotencyKey
import f95gm.server.http.response.*
import f95gm.server.model.http.OkResponse
import f95gm.server.routes.marks.live.invalidateLiveMark
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.application.IdempotencyRegistry
import f95gm.server.routes.marks.markDependencies
import f95gm.server.routes.marks.live.MarkRouteDependencies
import io.ktor.server.application.*

internal suspend fun deleteMark(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToUnmark()) ?: return
    val threadId = call.positiveThreadIdParameter("threadId")
    if (threadId == null) {
        respondFailure(
            call,
            f95gm.server.config.constants.HttpStatusCatalog.BAD_REQUEST,
            ApiFailure(UiMessages.server_invalidGameId(), ServerMessageKey.INVALID_GAME_ID)
        )
        return
    }
    val mutationKey = validIdempotencyKey(call.request.headers[HttpProtocolConfig.IDEMPOTENCY_KEY_HEADER])?.value
        ?.let { "delete-mark:${session.accountKey.value}:$it:${threadId.value}" }
    val lease = call.beginIdempotentMutation(mutationKey, threadId.value) ?: return
    lease.use {
        call.markDependencies.applicationServices.marks.removeAtomically(session.accountKey, threadId)
        invalidateLiveMark(call.markDependencies, threadId)
        val responseBody = encodeJson(OkResponse(true))
        it.complete(
            f95gm.server.application.port.CachedResponse(
                f95gm.domain.api.ApiStatusCode(f95gm.server.config.constants.HttpStatusCatalog.OK.value),
                responseBody,
                null
            )
        )
        respond(call, f95gm.server.config.constants.HttpStatusCatalog.OK, responseBody)
    }
}

