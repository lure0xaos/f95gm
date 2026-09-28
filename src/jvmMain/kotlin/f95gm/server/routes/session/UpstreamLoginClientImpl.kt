package f95gm.server.routes.session

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.upstream.request.*
import f95gm.server.http.response.encode
import f95gm.server.config.protocol.UpstreamProtocolConfig
import f95gm.server.upstream.request.UpstreamHeaders
import f95gm.server.upstream.request.followUpstreamRedirects
import f95gm.server.routes.ports.UpstreamLoginClient
import f95gm.server.routes.ports.UpstreamLoginResult
import f95gm.server.routes.ports.LoginName
import f95gm.server.routes.ports.LoginPassword
import io.ktor.http.*
import org.jsoup.Jsoup

internal class UpstreamLoginClientImpl : UpstreamLoginClient {
    override suspend fun authenticate(
        gateway: UpstreamRequester,
        login: LoginName,
        password: LoginPassword,
        browserId: F95SessionId
    ): UpstreamLoginResult = gateway.authenticateUpstream(login, password, browserId)
}

@JvmInline
private value class UpstreamFormToken(val value: String) {
    init { require(value.isNotBlank()) }
}

private suspend fun UpstreamRequester.authenticateUpstream(
    login: LoginName,
    password: LoginPassword,
    browserId: F95SessionId
): UpstreamLoginResult {
    val endpoints = this.endpointConfig
    val session = UpstreamSession(browserId, F95AccountKey(login.value.lowercase()))
    val loginPage = request(
        UpstreamRequests.get(
            UpstreamPaths.loginPage(),
            session,
            UpstreamHeaders.of(UpstreamHeader(HttpHeaders.Referrer, endpoints.origin))
            , policy = UpstreamEndpointPolicy.LOGIN
        )
    )
    val token = Jsoup.parse(loginPage.body).selectFirst("input[name=_xfToken]")?.attr("value")
        ?.takeIf(String::isNotBlank)?.let(::UpstreamFormToken)
        ?: return UpstreamLoginResult.MissingToken(session)

    val initialResponse = request(
        UpstreamRequests.post(
            UpstreamPaths.login(),
            session,
            loginForm(login, password, token, endpoints),
            headers = loginHeaders(endpoints)
            , policy = UpstreamEndpointPolicy.LOGIN
        )
    )
    val result = followUpstreamRedirects(this, initialResponse, session)
    val verified = request(
        UpstreamRequests.get(UpstreamPaths.latestPage(endpoints), session, policy = UpstreamEndpointPolicy.LOGIN)
    )
    return UpstreamLoginResult.Completed(session, initialResponse, result, verified)
}

private fun loginForm(
    login: LoginName,
    password: LoginPassword,
    token: UpstreamFormToken,
    endpointConfig: f95gm.server.config.upstream.UpstreamEndpointConfig
): String = listOf(
    "login" to login.value,
    "url" to "",
    "password" to password.value,
    "password_confirm" to "",
    "additional_security" to "",
    "remember" to "1",
    "website_code" to "",
    "_xfRedirect" to endpointConfig.origin,
    "_xfToken" to token.value
).joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }

private fun loginHeaders(endpointConfig: f95gm.server.config.upstream.UpstreamEndpointConfig) = UpstreamHeaders.of(
    UpstreamHeader(HttpHeaders.Referrer, endpointConfig.origin),
    UpstreamHeader(HttpHeaders.Origin, endpointConfig.origin),
    UpstreamHeader(HttpHeaders.AcceptLanguage, UpstreamProtocolConfig.ACCEPT_LANGUAGE_ENGLISH),
    UpstreamHeader(UpstreamProtocolConfig.SEC_FETCH_SITE, "same-origin"),
    UpstreamHeader(UpstreamProtocolConfig.SEC_FETCH_MODE, "navigate"),
    UpstreamHeader(UpstreamProtocolConfig.SEC_FETCH_USER, "?1"),
    UpstreamHeader(UpstreamProtocolConfig.SEC_FETCH_DEST, "document")
)
