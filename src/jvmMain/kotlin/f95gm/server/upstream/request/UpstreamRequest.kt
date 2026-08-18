package f95gm.server.upstream.request

import f95gm.server.config.session.setCookiePairs
import f95gm.server.config.upstream.upstream
import f95gm.server.config.upstream.upstreamUserAgent
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.persistence.sessions.persistSessionFor
import f95gm.server.state.runtime.client
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

internal suspend fun upstreamRequest(
    method: String,
    path: String,
    session: UpstreamSession,
    body: String? = null,
    contentType: String? = null,
    headers: Map<String, String> = emptyMap()
): UpstreamResponse {
    val uri = if (path.startsWith("http")) path else upstream + path
    val response = client.request(uri) {
        this.method = HttpMethod.parse(method)
        // F95zone's login protection treats the request as a browser flow.  A
        // product-specific or empty-looking user agent can be redirected
        // without receiving the authenticated xf_session cookie.
        header(HttpHeaders.UserAgent, upstreamUserAgent)
        header(HttpHeaders.Cookie, session.header())
        header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
        headers.forEach { (key, value) -> header(key, value) }
        if (contentType != null) contentType(ContentType.parse(contentType))
        if (body != null) setBody(body)
    }
    var cookiesChanged = false
    response.headers.getAll(HttpHeaders.SetCookie).orEmpty().forEach { headerValue ->
        setCookiePairs(headerValue).forEach { (name, cookieValue) ->
            if (session.cookies[name] != cookieValue) {
                session.cookies[name] = cookieValue
                cookiesChanged = true
            }
        }
    }
    if (cookiesChanged) persistSessionFor(session)
    return UpstreamResponse(
        status = response.status.value,
        body = response.bodyAsText(),
        contentType = response.headers[HttpHeaders.ContentType],
        location = response.headers[HttpHeaders.Location]
    )
}
