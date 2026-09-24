package f95gm.domain.api

import kotlinx.serialization.Serializable

@Serializable
data class F95ApiError(
    val message: String,
    val code: F95ApiErrorCode = F95ApiErrorCode.INTERNAL,
    val messageKey: F95ApiMessageKey? = null,
    val details: Map<String, String> = emptyMap()
)
