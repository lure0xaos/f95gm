package f95gm.server.app

import f95gm.server.http.response.ApiApplicationException
import f95gm.server.kernel.FailureCategory
import f95gm.server.kernel.failureCategory
import kotlinx.coroutines.CancellationException

/** Centralizes the intentionally broad request boundary into a typed monitoring failure. */
internal object RequestFailureAdapter {
    fun adapt(error: Throwable): RequestFailure {
        val kind = when {
            error is CancellationException -> RequestFailureKind.CANCELLED
            error is ApiApplicationException -> RequestFailureKind.API_FAILURE
            else -> RequestFailureKind.REQUEST_FAILED
        }
        return RequestFailure(kind, error, error.failureCategory())
    }
}
