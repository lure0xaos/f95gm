package f95gm.server.upstream.request

import f95gm.server.config.protocol.HttpProtocolConfig
import f95gm.server.config.protocol.UpstreamProtocolConfig
import f95gm.server.http.request.currentRequestIdValue
import f95gm.server.model.auth.UpstreamSession
import io.ktor.client.request.*
import io.ktor.http.*

internal fun HttpRequestBuilder.applyUpstreamTypedDefaults(
    method: HttpMethod,
    session: UpstreamSession,
    headers: List<UpstreamHeader>,
    contentType: UpstreamContentType?,
    body: String?
) {
    this.method = method
    header(UpstreamProtocolHeaders.USER_AGENT, UpstreamProtocolConfig.USER_AGENT)
    header(UpstreamProtocolHeaders.COOKIE, session.cookieSnapshot().header)
    header(UpstreamProtocolHeaders.ACCEPT, UpstreamProtocolHeaders.DEFAULT_ACCEPT)
    currentRequestIdValue()?.let { header(HttpProtocolConfig.REQUEST_ID_HEADER, it) }
    headers.forEach { header(it.name.text, it.value.text) }
    contentType?.let { contentType(ContentType.parse(it.text)) }
    body?.let(::setBody)
}

internal fun HttpRequestBuilder.applyUpstreamDefaults(
    method: HttpMethod,
    session: UpstreamSession,
    headers: List<UpstreamHeader>,
    contentTypeValue: String?,
    body: String?
) = applyUpstreamTypedDefaults(
    method, session, headers, contentTypeValue?.let(::UpstreamContentType), body
)

internal object UpstreamProtocolHeaders {
    const val USER_AGENT = "User-Agent"
    const val COOKIE = "Cookie"
    const val ACCEPT = "Accept"
    const val DEFAULT_ACCEPT = "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8"
}
