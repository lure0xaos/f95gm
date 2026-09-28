package f95gm.server.routes.filters

import f95gm.messages.UiMessages
import f95gm.server.config.protocol.HttpProtocolConfig
import f95gm.server.routes.ports.beginIdempotentMutation
import f95gm.server.http.request.validIdempotencyKey
import f95gm.server.http.response.*
import f95gm.server.model.http.OkResponse
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.application.IdempotencyRegistry
import f95gm.server.routes.filters.filterDependencies
import io.ktor.server.application.*

internal suspend fun deleteFilter(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToDeleteFilters()) ?: return
    val filterId = call.filterIdParameter()
    if (filterId == null) {
        respondFailure(
            call,
            f95gm.server.config.constants.HttpStatusCatalog.BAD_REQUEST,
            ApiFailure(UiMessages.server_invalidSavedFilterId(), ServerMessageKey.INVALID_FILTER_ID)
        )
        return
    }
    val mutationKey = validIdempotencyKey(call.request.headers[HttpProtocolConfig.IDEMPOTENCY_KEY_HEADER])?.value
        ?.let { "delete-filter:${session.accountKey.value}:$it:${filterId.value}" }
    val lease = call.beginIdempotentMutation(mutationKey, filterId.value) ?: return
    lease.use {
        call.filterDependencies.applicationServices.savedFilters.removeAtomically(session.accountKey, filterId)
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

