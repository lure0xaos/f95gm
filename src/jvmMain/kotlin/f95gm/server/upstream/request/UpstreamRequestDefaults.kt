package f95gm.server.upstream.request

import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.upstream.upstreamUserAgent
import f95gm.server.http.request.currentRequestContext
import f95gm.server.model.auth.UpstreamSession
import io.ktor.client.request.*
import io.ktor.http.*

internal fun HttpRequestBuilder.applyUpstreamDefaults(
    method: HttpMethod,
    session: UpstreamSession,
    headers: List<UpstreamHeader>,
    contentTypeValue: String?,
    body: String?
) {
    this.method = method
    header(HttpHeaders.UserAgent, upstreamUserAgent)
    header(HttpHeaders.Cookie, session.header())
    header(HttpHeaders.Accept, UpstreamProtocolHeaders.DEFAULT_ACCEPT)
    currentRequestContext()?.requestId?.value?.let { header(ServerConstants.REQUEST_ID_HEADER, it) }
    headers.forEach { header(it.name, it.value) }
    contentTypeValue?.let { contentType(ContentType.parse(it)) }
    body?.let(::setBody)
}

internal object UpstreamProtocolHeaders {
    const val DEFAULT_ACCEPT = "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8"
}
