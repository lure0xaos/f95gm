package f95gm.server.http.response

internal data class ApiFailureDetails(
    val fieldName: String? = null,
    val reason: String? = null,
    val status: Int? = null
) {
    fun asMap(): Map<String, String> = buildMap {
        fieldName?.let { put("field", it) }
        reason?.let { put("reason", it) }
        status?.let { put("status", it.toString()) }
    }

    companion object {
        val EMPTY = ApiFailureDetails()
        fun field(name: String): ApiFailureDetails = ApiFailureDetails(fieldName = name)
        fun reason(value: String): ApiFailureDetails = ApiFailureDetails(reason = value)
        fun status(value: Int): ApiFailureDetails = ApiFailureDetails(status = value)
    }
}
