package f95gm.server.routes.session

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95HttpStatus
import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.logging.*
import f95gm.server.config.upstream.upstream
import f95gm.server.http.auth.isAuthenticatedUpstreamResponse
import f95gm.server.http.auth.isUnauthenticatedUpstreamBody
import f95gm.server.http.auth.upstreamLoginMessage
import f95gm.server.http.logging.redactLogValue
import f95gm.server.http.request.currentRequestContext
import f95gm.server.http.request.decodeJsonOrNull
import f95gm.server.http.request.receiveLimitedText
import f95gm.server.http.request.requestField
import f95gm.server.http.response.*
import f95gm.server.model.auth.LoginRequest
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.OkResponse
import f95gm.server.state.runtime.routeDependencies
import f95gm.server.upstream.request.UpstreamHeaders
import f95gm.server.upstream.request.followUpstreamRedirects
import f95gm.server.upstream.request.upstreamRequest
import io.ktor.http.*
import io.ktor.server.application.*
import org.jsoup.Jsoup
import kotlin.uuid.Uuid

internal suspend fun login(call: ApplicationCall) {
    val body = call.receiveLimitedText(ServerConstants.MAX_REQUEST_BODY_BYTES)
    val jsonRequest = decodeJsonOrNull<LoginRequest>(body)
    if (body.trimStart().startsWith("{") && jsonRequest == null) {
        respondFailure(
            call,
            ServerConstants.STATUS_BAD_REQUEST,
            ApiFailure("Invalid JSON request", ServerMessageKeys.INVALID_JSON_REQUEST, F95ApiErrorCode.INVALID_INPUT)
        )
        return
    }
    val login = (jsonRequest?.login ?: requestField(body, "login")).orEmpty().trim()
    val password = (jsonRequest?.password ?: requestField(body, "password")).orEmpty()
    if (login.isBlank() || password.isBlank()) {
        respondFailure(
            call,
            ServerConstants.STATUS_BAD_REQUEST,
            ApiFailure(UiMessages.server_credentialsRequired(), ServerMessageKeys.CREDENTIALS_REQUIRED)
        )
        return
    }

    val browserId = F95SessionId(Uuid.random().toString())
    val upstreamSession = UpstreamSession(browserId = browserId, accountKey = F95AccountKey(login.lowercase()))
    val loginPage = call.routeDependencies.upstreamRequest(
        method = HttpMethod.Get,
        path = ServerConstants.UPSTREAM_LOGIN_PAGE,
        session = upstreamSession,
        headers = UpstreamHeaders.of(mapOf(HttpHeaders.Referrer to upstream))
    )
    val token = Jsoup.parse(loginPage.body).selectFirst("input[name=_xfToken]")?.attr("value")
    if (token.isNullOrBlank()) {
        respondFailure(
            call,
            ServerConstants.STATUS_BAD_GATEWAY,
            ApiFailure(UiMessages.server_loginTokenMissing(), ServerMessageKeys.LOGIN_TOKEN_MISSING)
        )
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
    val loginResponse = call.routeDependencies.upstreamRequest(
        method = HttpMethod.Post,
        path = ServerConstants.UPSTREAM_LOGIN,
        session = upstreamSession,
        body = form,
        contentType = "application/x-www-form-urlencoded",
        headers = UpstreamHeaders.of(
            mapOf(
            HttpHeaders.Referrer to upstream,
            HttpHeaders.Origin to upstream,
            HttpHeaders.AcceptLanguage to "en-US,en;q=0.9",
            "Sec-Fetch-Site" to "same-origin",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-User" to "?1",
            "Sec-Fetch-Dest" to "document"
            )
        )
    )
    val result = followUpstreamRedirects(call.routeDependencies, loginResponse, upstreamSession)
    val verified = call.routeDependencies.upstreamRequest(HttpMethod.Get, "/sam/latest_alpha/", upstreamSession)
    if (call.respondUpstreamFailure(result) || call.respondUpstreamFailure(verified)) return
    val verifiedSession = isAuthenticatedUpstreamResponse(verified)
    if (result.status.value !in F95HttpStatus.SUCCESS_MIN..ServerConstants.REDIRECT_STATUS_MAX || !verifiedSession) {
        val upstreamMessage = upstreamLoginMessage(result.body)
        logger.warn {
            "F95zone login verification failed: initialStatus=${loginResponse.status}, " +
                    "initialLocation=${loginResponse.location?.substringBefore('?') ?: "none"}, finalStatus=${result.status.value}, " +
                    "verificationStatus=${verified.status}, cookies=${
                        upstreamSession.cookieEntries().keys.joinToString(
                            ","
                        )
                    }, " +
                    "finalUnauthenticated=${isUnauthenticatedUpstreamBody(result.body)}, " +
                    "verificationUnauthenticated=${isUnauthenticatedUpstreamBody(verified.body)}, " +
                    "message=${redactLogValue(upstreamMessage ?: "none")}"
        }
        logger.info {
            auditLog(
                AuditEvent(
                    AuditEventName.LOGIN_FAILED, listOf(
                        logField("requestId", currentRequestContext()?.requestId?.value),
                        logField("account", redactLogValue(login)),
                        logField("outcome", "failure")
                    )
                )
            )
        }
        val message = if (upstreamMessage?.contains("incorrect", ignoreCase = true) == true) {
            UiMessages.server_credentialsRejected()
        } else if (!upstreamMessage.isNullOrBlank()) {
            upstreamMessage
        } else if (!verifiedSession) {
            if (ServerConstants.UPSTREAM_SESSION_COOKIE !in upstreamSession.cookieEntries()) {
                UiMessages.server_sessionNotConfirmedCookie(
                    loginResponse.location?.substringBefore('?') ?: "none",
                    verified.status
                )
            } else {
                UiMessages.server_sessionNotConfirmedPage(
                    loginResponse.location?.substringBefore('?') ?: "none",
                    verified.status
                )
            }
        } else {
            UiMessages.server_loginFailed(result.status.value)
        }
        respondFailure(call, ServerConstants.STATUS_UNAUTHORIZED, ApiFailure(message, ServerMessageKeys.LOGIN_FAILED))
        return
    }

    call.routeDependencies.sessions.put(browserId, upstreamSession)
    call.routeDependencies.sessionService.persist(browserId, upstreamSession)
    logger.info {
        auditLog(
            AuditEvent(
                AuditEventName.LOGIN_SUCCEEDED, listOf(
                    logField("requestId", currentRequestContext()?.requestId?.value),
                    logField("account", redactLogValue(login)),
                    logField("outcome", "success")
                )
            )
        )
    }
    setBrowserCookie(call, browserId)
    respondJson(call, ServerConstants.STATUS_OK, OkResponse(true))
}
