package f95gm.domain.api

import kotlinx.serialization.Serializable

@Serializable
data class F95ApiError(
    val message: String,
    val code: F95ApiErrorCode = F95ApiErrorCode.INTERNAL,
    val messageKey: F95ApiMessageKey? = null,
    val details: Map<String, String> = emptyMap()
) {
    val typedDetails: ApiErrorDetailsContract get() = ApiErrorDetailsContract.fromMap(details)

    companion object {
        fun fromDetails(
            message: String,
            code: F95ApiErrorCode = F95ApiErrorCode.INTERNAL,
            messageKey: F95ApiMessageKey? = null,
            details: ApiErrorDetailsContract = ApiErrorDetailsContract()
        ): F95ApiError = F95ApiError(message, code, messageKey, details.toWireMap())
    }
}
