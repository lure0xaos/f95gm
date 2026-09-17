package f95gm.server.app

import f95gm.domain.api.F95ApiErrorCode
import f95gm.server.http.response.ApiApplicationException
import f95gm.server.http.response.ApiFailureKind

internal object ApiErrorPolicy {
    fun code(error: ApiApplicationException): F95ApiErrorCode = when {
        error.code != F95ApiErrorCode.INTERNAL -> error.code
        error.kind == ApiFailureKind.Upstream -> F95ApiErrorCode.UPSTREAM_FAILURE
        error.kind == ApiFailureKind.Validation -> F95ApiErrorCode.INVALID_INPUT
        else -> F95ApiErrorCode.INTERNAL
    }
}
