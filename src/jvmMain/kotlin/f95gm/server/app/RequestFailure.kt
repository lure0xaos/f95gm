package f95gm.server.app

import f95gm.server.kernel.FailureCategory


internal enum class RequestFailureKind {
    API_FAILURE,
    REQUEST_FAILED,
    CANCELLED
}

internal data class RequestFailure(
    val kind: RequestFailureKind,
    val cause: Throwable,
    val category: FailureCategory
) {
    companion object {
        fun from(error: Throwable): RequestFailure = RequestFailureAdapter.adapt(error)
    }
}
