package f95gm.server.http.response

import f95gm.domain.api.ApiErrorDetailsContract
import f95gm.domain.api.ApiStatusCode
import f95gm.server.model.EpochMillis

@JvmInline
internal value class ApiErrorDetailKey(val value: String)

@JvmInline
internal value class ApiFieldName(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
internal value class ApiErrorReason(val value: String) {
    init { require(value.isNotBlank()) }
}

internal data class ApiFailureDetails(
    val fieldName: String? = null,
    val reason: String? = null,
    val status: ApiStatusCode? = null,
    val expectedUpdatedAt: EpochMillis? = null,
    val actualUpdatedAt: EpochMillis? = null
) {
    val typedFieldName: ApiFieldName? get() = fieldName?.let(::ApiFieldName)
    val typedReason: ApiErrorReason? get() = reason?.let(::ApiErrorReason)

    fun asMap(): Map<String, String> = buildMap {
        fieldName?.let { put(ApiErrorDetailKeys.FIELD.value, it) }
        reason?.let { put(ApiErrorDetailKeys.REASON.value, it) }
        status?.let { put(ApiErrorDetailKeys.STATUS.value, it.value.toString()) }
        expectedUpdatedAt?.let { put(ApiErrorDetailKeys.EXPECTED_UPDATED_AT.value, it.value.toString()) }
        actualUpdatedAt?.let { put(ApiErrorDetailKeys.ACTUAL_UPDATED_AT.value, it.value.toString()) }
    }

    fun asContract(): ApiErrorDetailsContract = ApiErrorDetailsContract(
        fieldName, reason, status?.value, expectedUpdatedAt, actualUpdatedAt
    )

    companion object {
        val EMPTY = ApiFailureDetails()
        fun field(name: String): ApiFailureDetails = ApiFailureDetails(fieldName = name)
        fun field(name: ApiFieldName): ApiFailureDetails = ApiFailureDetails(fieldName = name.value)
        fun reason(value: String): ApiFailureDetails = ApiFailureDetails(reason = value)
        fun reason(value: ApiErrorReason): ApiFailureDetails = ApiFailureDetails(reason = value.value)
        fun status(value: Int): ApiFailureDetails = ApiFailureDetails(status = ApiStatusCode(value))
        fun conflict(expected: EpochMillis?, actual: EpochMillis?): ApiFailureDetails = ApiFailureDetails(
            reason = "conditional_write_conflict", expectedUpdatedAt = expected, actualUpdatedAt = actual
        )
        fun conflict(expected: Long?, actual: Long?): ApiFailureDetails = conflict(
            expected?.let(::EpochMillis), actual?.let(::EpochMillis)
        )
    }
}

private object ApiErrorDetailKeys {
    val FIELD = ApiErrorDetailKey("field")
    val REASON = ApiErrorDetailKey("reason")
    val STATUS = ApiErrorDetailKey("status")
    val EXPECTED_UPDATED_AT = ApiErrorDetailKey("expectedUpdatedAt")
    val ACTUAL_UPDATED_AT = ApiErrorDetailKey("actualUpdatedAt")
}
