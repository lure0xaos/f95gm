package f95gm.server.routes.filters

import f95gm.domain.api.F95ApiErrorCode
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.request.*
import f95gm.server.http.response.*
import f95gm.server.model.filters.SavedFilterRequest
import f95gm.server.persistence.ConditionalWriteResult
import f95gm.server.persistence.filters.savedFiltersJson
import f95gm.server.persistence.filters.toSavedFilter
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.state.runtime.routeDependencies
import io.ktor.server.application.*

internal suspend fun saveFilter(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToSaveFilters()) ?: return
    val idempotencyKey = validIdempotencyKey(call.request.headers[ServerConstants.IDEMPOTENCY_KEY_HEADER])
    val mutationKey = idempotencyKey?.value?.let { "filter:${session.accountKey.value}:$it" }
    val body = call.receiveLimitedText(ServerConstants.MAX_REQUEST_BODY_BYTES)
    val request = when (val decoded = decodeJson<SavedFilterRequest>(body)) {
        is f95gm.server.http.request.RequestValidation.Valid -> decoded.value
        is f95gm.server.http.request.RequestValidation.Invalid -> {
            respondFailure(
                call,
                ServerConstants.STATUS_BAD_REQUEST,
                decoded.toApiFailure()
            )
            return
        }
    }
    val filter = request.toSavedFilter(call.routeDependencies.clock)
    if (filter == null) {
        respondFailure(
            call,
            ServerConstants.STATUS_BAD_REQUEST,
            ApiFailure(
                UiMessages.server_savedFilterNameRequired(),
                ServerMessageKeys.SAVED_FILTER_NAME_REQUIRED,
                F95ApiErrorCode.INVALID_INPUT,
                ApiFailureDetails.field("name")
            )
        )
        return
    }
    val lease = call.beginIdempotentMutation(mutationKey, body) ?: return
    lease.use {
        val writeResult = call.routeDependencies.applicationServices.savedFilters.storeIfCurrent(
            session.accountKey,
            filter,
            request.expectedUpdatedAt?.value
        )
        if (writeResult is ConditionalWriteResult.Conflict) {
            it.release()
            respondApiFailure(
                call,
                ServerConstants.STATUS_CONFLICT,
                UiMessages.server_savedFilterConflict(),
                ServerMessageKeys.SAVED_FILTER_CONFLICT,
                F95ApiErrorCode.INVALID_INPUT
            )
            return
        }
        val storedFilter = (writeResult as ConditionalWriteResult.Stored).value
        val responseBody = savedFiltersJson(listOf(storedFilter))
        it.complete(
            f95gm.server.state.runtime.IdempotencyRegistry.CachedResponse(
                ServerConstants.STATUS_OK.value,
                responseBody,
                null
            )
        )
        respond(call, ServerConstants.STATUS_OK, responseBody)
    }
}
