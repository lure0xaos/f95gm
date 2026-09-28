package f95gm.server.upstream.request

import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.config.protocol.UpstreamProtocolConfig
import f95gm.server.config.upstream.UpstreamEndpointConfig
import f95gm.domain.values.F95ThreadId
import io.ktor.http.HttpMethod

internal data class UpstreamHeaderName(val value: String) {
    val text: String get() = value
    init { require(value.isNotBlank() && value.all { it.code in 33..126 && it != ':' }) }
}

internal data class UpstreamHeaderValue(val value: String) {
    val text: String get() = value
    init { require(value.length <= 8_192 && value.none { it == '\r' || it == '\n' || it.code < 0x20 || it.code == 0x7f }) }
}

internal data class UpstreamHeader(val name: UpstreamHeaderName, val value: UpstreamHeaderValue) {
    constructor(name: String, value: String) : this(UpstreamHeaderName(name), UpstreamHeaderValue(value))
}

@JvmInline
internal value class UpstreamBody(val value: String) {
    val text: String get() = value
}

@JvmInline
internal value class UpstreamFlightKey(val value: String)

@JvmInline
internal value class UpstreamCircuitKey(val value: String)

@JvmInline
internal value class UpstreamPath(val value: String) {
    val text: String get() = value
}

/** Fully resolved URI accepted by the transport boundary. */
@JvmInline
internal value class UpstreamRequestUri(val value: String) {
    init { require(value.isNotBlank()) }
}

internal fun upstreamPath(value: String, baseUrl: String): UpstreamPath =
    UpstreamPath(value.removePrefix(baseUrl).ifBlank { "/" })

internal object UpstreamPaths {
    fun latestPage(endpoints: UpstreamEndpointConfig): UpstreamPath = upstreamPath(endpoints.latestPage, endpoints.origin)
    fun sessionStatus(endpoints: UpstreamEndpointConfig): UpstreamPath = latestPage(endpoints)
    fun catalog(query: String, endpoints: UpstreamEndpointConfig): UpstreamPath =
        upstreamPath("${endpoints.latestApi}?$query", endpoints.origin)
    fun loginPage(): UpstreamPath = UpstreamPath(UpstreamProtocolConfig.LOGIN_PAGE)
    fun login(): UpstreamPath = UpstreamPath(UpstreamProtocolConfig.LOGIN)
    fun thread(threadId: F95ThreadId): UpstreamPath = UpstreamPath("/threads/${threadId.value}/")
}

@JvmInline
internal value class UpstreamContentType(val value: String) {
    val text: String get() = value
    companion object {
        val FORM_URLENCODED = UpstreamContentType(UpstreamProtocolConfig.CONTENT_TYPE_FORM_URLENCODED)
    }
}

internal data class UpstreamRequest(
    val method: HttpMethod,
    val path: UpstreamPath,
    val session: UpstreamSession,
    val body: UpstreamBody? = null,
    val contentType: UpstreamContentType? = null,
    val headers: UpstreamHeaders = UpstreamHeaders.EMPTY,
    val policy: UpstreamPolicySelection = UpstreamEndpointPolicy.DEFAULT
) {
    fun context(): UpstreamRequestContext = UpstreamRequestContext(
        session, body, contentType, headers.values, policy
    )
}

internal data class UpstreamRequestContext(
    val session: UpstreamSession,
    val body: UpstreamBody?,
    val contentType: UpstreamContentType?,
    val headers: List<UpstreamHeader>,
    val policy: UpstreamPolicySelection
)

/** Typed construction boundary for outbound requests. */
internal object UpstreamRequests {
    fun get(
        path: UpstreamPath,
        session: UpstreamSession,
        headers: UpstreamHeaders = UpstreamHeaders.EMPTY,
        policy: UpstreamPolicySelection = UpstreamEndpointPolicy.DEFAULT
    ) = UpstreamRequest(HttpMethod.Get, path, session, headers = headers, policy = policy)

    fun post(
        path: UpstreamPath,
        session: UpstreamSession,
        body: String,
        contentType: UpstreamContentType = UpstreamContentType.FORM_URLENCODED,
        headers: UpstreamHeaders = UpstreamHeaders.EMPTY,
        policy: UpstreamPolicySelection = UpstreamEndpointPolicy.DEFAULT
    ) = UpstreamRequest(HttpMethod.Post, path, session, UpstreamBody(body), contentType, headers, policy)
}

internal class UpstreamHeaders private constructor(
    val values: List<UpstreamHeader>
) {
    init {
        require(values.none { it.name.value.isBlank() }) { "Upstream headers must contain non-blank names" }
        require(values.map { it.name.value.lowercase() }.distinct().size == values.size) {
            "Upstream headers must not contain duplicate names"
        }
    }

    companion object {
        val EMPTY = UpstreamHeaders(emptyList())

        fun of(vararg values: UpstreamHeader): UpstreamHeaders = UpstreamHeaders(values.toList())
    }
}

internal interface UpstreamRequester {
    val endpointConfig: UpstreamEndpointConfig
    suspend fun request(request: UpstreamRequest): UpstreamResponse
}

/** Concrete transport gateway; route/application code depends only on [UpstreamRequester]. */
internal interface UpstreamGateway : UpstreamRequester
