package f95gm.server.routes.filters

import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.request.beginIdempotentMutation
import f95gm.server.http.request.filterIdParameter
import f95gm.server.http.request.validIdempotencyKey
import f95gm.server.http.response.*
import f95gm.server.model.http.OkResponse
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.state.runtime.IdempotencyRegistry
import f95gm.server.state.runtime.routeDependencies
import io.ktor.server.application.*

internal suspend fun deleteFilter(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToDeleteFilters()) ?: return
    val filterId = call.filterIdParameter()
    if (filterId == null) {
        respondFailure(
            call,
            ServerConstants.STATUS_BAD_REQUEST,
            ApiFailure(UiMessages.server_invalidSavedFilterId(), ServerMessageKeys.INVALID_FILTER_ID)
        )
        return
    }
    val mutationKey = validIdempotencyKey(call.request.headers[ServerConstants.IDEMPOTENCY_KEY_HEADER])?.value
        ?.let { "delete-filter:${session.accountKey.value}:$it:${filterId.value}" }
    val lease = call.beginIdempotentMutation(mutationKey, filterId.value) ?: return
    lease.use {
        call.routeDependencies.applicationServices.savedFilters.remove(session.accountKey, filterId)
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
