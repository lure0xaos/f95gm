package f95gm.server.routes.session

import f95gm.domain.api.F95HttpStatus
import f95gm.domain.values.F95AccountKey
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.logging.logger
import f95gm.server.config.upstream.upstream
import f95gm.server.http.auth.isAuthenticatedUpstreamResponse
import f95gm.server.http.auth.isUnauthenticatedUpstreamBody
import f95gm.server.http.auth.upstreamLoginMessage
import f95gm.server.http.request.requestField
import f95gm.server.http.response.encode
import f95gm.server.http.response.jsonMessage
import f95gm.server.http.response.respond
import f95gm.server.model.auth.LoginRequest
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.OkResponse
import f95gm.server.persistence.sessions.persistSession
import f95gm.server.state.runtime.sessions
import f95gm.server.state.runtime.upstreamJson
import f95gm.server.upstream.request.followUpstreamRedirects
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import org.jsoup.Jsoup
import kotlin.uuid.Uuid

internal suspend fun login(call: ApplicationCall) {
    val body = call.receiveText()
    val jsonRequest = runCatching { upstreamJson.decodeFromString<LoginRequest>(body) }.getOrNull()
    val login = (jsonRequest?.login ?: requestField(body, "login")).orEmpty().trim()
    val password = (jsonRequest?.password ?: requestField(body, "password")).orEmpty()
    if (login.isBlank() || password.isBlank()) {
        respond(call, ServerConstants.STATUS_BAD_REQUEST, jsonMessage(UiMessages.server_credentialsRequired()))
        return
    }

    val upstreamSession = UpstreamSession(accountKey = F95AccountKey(login.lowercase()))
    val loginPage = upstreamRequest(
        method = "GET",
        path = ServerConstants.UPSTREAM_LOGIN_PAGE,
        session = upstreamSession,
        headers = mapOf(HttpHeaders.Referrer to upstream)
    )
    val token = Jsoup.parse(loginPage.body).selectFirst("input[name=_xfToken]")?.attr("value")
    if (token.isNullOrBlank()) {
        respond(call, ServerConstants.STATUS_BAD_GATEWAY, jsonMessage(UiMessages.server_loginTokenMissing()))
        return
    }

    val form = listOf(
        "login" to login,
        "url" to "",
        "password" to password,
        "password_confirm" to "",
        "additional_security" to "",
        "remember" to "1",
        "website_code" to "",
        "_xfRedirect" to upstream,
        "_xfToken" to token
    ).joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
    val loginResponse = upstreamRequest(
        method = "POST",
        path = ServerConstants.UPSTREAM_LOGIN,
        session = upstreamSession,
        body = form,
        contentType = "application/x-www-form-urlencoded",
        headers = mapOf(
            HttpHeaders.Referrer to upstream,
            HttpHeaders.Origin to upstream,
            HttpHeaders.AcceptLanguage to "en-US,en;q=0.9",
            "Sec-Fetch-Site" to "same-origin",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-User" to "?1",
            "Sec-Fetch-Dest" to "document"
        )
    )
    val result = followUpstreamRedirects(loginResponse, upstreamSession)
    val verified = upstreamRequest("GET", "/sam/latest_alpha/", upstreamSession)
    val verifiedSession = isAuthenticatedUpstreamResponse(verified)
    if (result.status !in F95HttpStatus.SUCCESS_MIN..ServerConstants.REDIRECT_STATUS_MAX || !verifiedSession) {
        val upstreamMessage = upstreamLoginMessage(result.body)
        logger.warn {
            "F95zone login verification failed: initialStatus=${loginResponse.status}, " +
                    "initialLocation=${loginResponse.location ?: "none"}, finalStatus=${result.status}, " +
                    "verificationStatus=${verified.status}, cookies=${upstreamSession.cookies.keys.joinToString(",")}, " +
                    "finalUnauthenticated=${isUnauthenticatedUpstreamBody(result.body)}, " +
                    "verificationUnauthenticated=${isUnauthenticatedUpstreamBody(verified.body)}, " +
                    "message=${upstreamMessage ?: "none"}"
        }
        val message = if (upstreamMessage?.contains("incorrect", ignoreCase = true) == true) {
            UiMessages.server_credentialsRejected()
        } else if (!upstreamMessage.isNullOrBlank()) {
            upstreamMessage
        } else if (!verifiedSession) {
            if (ServerConstants.UPSTREAM_SESSION_COOKIE !in upstreamSession.cookies) {
                UiMessages.server_sessionNotConfirmedCookie(
                    loginResponse.location ?: "none",
                    verified.status
                )
            } else {
                UiMessages.server_sessionNotConfirmedPage(
                    loginResponse.location ?: "none",
                    verified.status
                )
            }
        } else {
            UiMessages.server_loginFailed(result.status)
        }
        respond(call, ServerConstants.STATUS_UNAUTHORIZED, jsonMessage(message))
        return
    }

    val browserId = Uuid.random().toString()
    sessions[browserId] = upstreamSession
    persistSession(browserId, upstreamSession)
    setBrowserCookie(call, browserId)
    respond(call, F95HttpStatus.SUCCESS_MIN, upstreamJson.encodeToString(OkResponse(true)))
}
