package f95gm.server.routes.filters

import f95gm.domain.api.F95ApiErrorCode
import f95gm.messages.UiMessages
import f95gm.server.config.protocol.HttpProtocolConfig
import f95gm.server.http.request.*
import f95gm.server.routes.ports.beginIdempotentMutation
import f95gm.server.http.response.*
import f95gm.domain.api.SavedFilterRequest
import f95gm.server.model.ConditionalWriteResult
import f95gm.server.model.EpochMillis
import f95gm.server.http.response.savedFiltersJson
import f95gm.server.routes.session.requireBrowserSession
import f95gm.server.application.IdempotencyRegistry
import f95gm.server.routes.filters.filterDependencies
import io.ktor.server.application.*
import f95gm.domain.api.ApiStatusCode
import f95gm.server.application.port.CachedResponse
import f95gm.server.config.constants.HttpStatusCatalog

internal suspend fun saveFilter(call: ApplicationCall) {
    val session = call.requireBrowserSession(UiMessages.server_signInToSaveFilters()) ?: return
    val dependencies = call.filterDependencies
    val idempotencyKey = validIdempotencyKey(call.request.headers[HttpProtocolConfig.IDEMPOTENCY_KEY_HEADER])
    val mutationKey = idempotencyKey?.value?.let { "filter:${session.accountKey.value}:$it" }
    val received = call.receiveMutationRequest<SavedFilterRequest>(dependencies.maxRequestBodyBytes.value) ?: return
    val body = received.body
    val request = received.request
    val filter = dependencies.applicationServices.savedFilters.fromRequest(
        request, dependencies.clock
    )
    if (filter == null) {
        respondFailure(
            call,
            badRequestResponse(ApiFailure(
                UiMessages.server_savedFilterNameRequired(),
                ServerMessageKey.SAVED_FILTER_NAME_REQUIRED,
                F95ApiErrorCode.INVALID_INPUT,
                ApiFailureDetails.field("name")
            ))
        )
        return
    }
    val lease = call.beginIdempotentMutation(mutationKey, body) ?: return
    lease.use {
        val writeResult = dependencies.applicationServices.savedFilters.storeAtomically(
            session.accountKey,
            filter,
            request.expectedUpdatedAt?.value?.let(::EpochMillis)
        )
        val storedFilter = when (writeResult) {
            is ConditionalWriteResult.Conflict -> {
                it.release()
                val failure = conflictResponse(
                    MutationResponsePolicy.FILTER, writeResult.expectedUpdatedAt, writeResult.actualUpdatedAt
                )
                respondFailure(call, failure)
                return
            }
            is ConditionalWriteResult.Stored -> writeResult.value
        }
        val responseBody = savedFiltersJson(listOf(storedFilter))
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


