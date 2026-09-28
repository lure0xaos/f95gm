package f95gm.domain.api

/** Platform-neutral response envelope shared by API clients and contract tests. */
data class ApiResponseEnvelope<T>(val statusCode: ApiStatusCode, val body: T) {
    val status: Int get() = statusCode.value

    fun <R> mapBody(transform: (T) -> R): ApiResponseEnvelope<R> = ApiResponseEnvelope(statusCode, transform(body))
}
