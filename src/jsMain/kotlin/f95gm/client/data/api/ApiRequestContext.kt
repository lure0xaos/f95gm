package f95gm.client.data.api

import f95gm.domain.api.ApiPath
import f95gm.client.model.runtime.HttpStatusCodeValue
import f95gm.client.model.runtime.ApiEndpointId
import io.ktor.http.HttpMethod

internal value class ApiRequestBody(val value: String)

internal value class ApiContentType(val value: String)

internal value class ApiHeaderName(val value: String) {
    init { require(value.isNotBlank() && value.none { it == '\r' || it == '\n' }) }
}

internal value class ApiHeaderValue(val value: String) {
    init { require(value.none { it == '\r' || it == '\n' }) }
}

internal data class ApiHeader(val name: ApiHeaderName, val value: ApiHeaderValue)

internal class ApiHeaders private constructor(val values: List<ApiHeader>) {
    init {
        require(values.map { it.name.value.lowercase() }.distinct().size == values.size) {
            "API headers must not contain duplicate names"
        }
    }

    companion object {
        val EMPTY = ApiHeaders(emptyList())

        fun of(vararg values: ApiHeader): ApiHeaders = ApiHeaders(values.toList())
        fun of(values: Map<String, String>): ApiHeaders = ApiHeaders(
            values.map { (name, value) -> ApiHeader(ApiHeaderName(name), ApiHeaderValue(value)) }
        )
    }
}

/** Complete request description passed through the client transport boundary. */
internal data class ApiRequestContext(
    val path: ApiPath,
    val endpoint: ApiEndpointId = ApiEndpointId(path.value),
    val method: HttpMethod,
    val body: ApiRequestBody? = null,
    val contentType: ApiContentType? = null,
    val headers: ApiHeaders = ApiHeaders.EMPTY,
    val fallback: ApiStatusFallback,
    val retryPolicy: RetryPolicy = RetryPolicy()
)
