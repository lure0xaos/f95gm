package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import f95gm.messages.UiMessages
import f95gm.server.model.http.UpstreamFailure
import f95gm.server.model.EpochMillis
import io.ktor.http.HttpStatusCode

/** Central wire-error definitions used by infrastructure-to-HTTP adapters. */
internal object ApiFailureCatalog {
    private data class Definition(
        val status: HttpStatusCode,
        val message: String,
        val key: ServerMessageKey,
        val code: F95ApiErrorCode
    ) {
        fun failure(details: ApiFailureDetails = ApiFailureDetails.EMPTY) = ApiFailure(message, key, code, details)
        fun response(details: ApiFailureDetails = ApiFailureDetails.EMPTY) = ApiFailureResponse(status, failure(details))
    }

    private val UPSTREAM = Definition(HttpStatusCode.BadGateway, UiMessages.server_upstreamFailure(), ServerMessageKey.UPSTREAM_FAILURE, F95ApiErrorCode.UPSTREAM_FAILURE)
    private val FILTER_CONFLICT = Definition(HttpStatusCode.Conflict, UiMessages.server_savedFilterConflict(), ServerMessageKey.SAVED_FILTER_CONFLICT, F95ApiErrorCode.INVALID_INPUT)
    private val MARK_CONFLICT = Definition(HttpStatusCode.Conflict, UiMessages.server_markConflict(), ServerMessageKey.MARK_CONFLICT, F95ApiErrorCode.INVALID_INPUT)

    fun upstream(failure: UpstreamFailure): ApiFailure = UPSTREAM.failure(ApiFailureDetails.reason(ApiErrorReason(failure.name)))

    fun mutationConflict(resource: MutationResponsePolicy): ApiFailure = definition(resource).failure()

    fun mutationConflictResponse(
        resource: MutationResponsePolicy,
        expectedUpdatedAt: EpochMillis?,
        actualUpdatedAt: EpochMillis?
    ): ApiFailureResponse = definition(resource).response(ApiFailureDetails.conflict(expectedUpdatedAt, actualUpdatedAt))

    private fun definition(resource: MutationResponsePolicy): Definition = when (resource) {
        MutationResponsePolicy.FILTER -> FILTER_CONFLICT
        MutationResponsePolicy.MARK -> MARK_CONFLICT
    }
}
