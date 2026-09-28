package f95gm.server.http.response

import f95gm.server.model.EpochMillis

internal enum class MutationResponsePolicy { FILTER, MARK }

internal fun conflictResponse(
    resource: MutationResponsePolicy,
    expectedUpdatedAt: EpochMillis? = null,
    actualUpdatedAt: EpochMillis? = null
): ApiFailureResponse {
    return ApiFailureCatalog.mutationConflictResponse(resource, expectedUpdatedAt, actualUpdatedAt)
}

