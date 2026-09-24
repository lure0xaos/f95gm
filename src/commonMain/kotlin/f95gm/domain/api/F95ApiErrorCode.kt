package f95gm.domain.api

import kotlinx.serialization.Serializable

@Serializable
enum class F95ApiErrorCode {
    INVALID_INPUT,
    REQUEST_TOO_LARGE,
    CSRF_REJECTED,
    RATE_LIMITED,
    UPSTREAM_FAILURE,
    INTERNAL
}
