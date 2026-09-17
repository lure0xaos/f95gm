package f95gm.server.routes.session

import f95gm.domain.values.F95SessionId
import f95gm.messages.UiMessages
import f95gm.server.config.session.parseSessionCookieResult
import f95gm.server.http.response.ApiFailure
import f95gm.server.http.response.ServerMessageKey
import f95gm.server.http.response.respondFailure
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.routes.session.sessionDependencies
import f95gm.server.application.SessionRegistry
import f95gm.server.session.SessionLookup
import io.ktor.http.*
import io.ktor.server.application.*
import f95gm.server.config.constants.HttpStatusCatalog
import f95gm.server.config.session.SessionCookieParseResult

internal sealed interface BrowserSession {
    data object MissingCookie : BrowserSession
    data object MissingRegistrySession : BrowserSession
    data class Found(val id: F95SessionId, val session: UpstreamSession) : BrowserSession
}

internal fun browserSession(call: ApplicationCall, registry: SessionRegistry): BrowserSession {
    return when (val cookie = parseSessionCookieResult(call.request.headers[HttpHeaders.Cookie])) {
        SessionCookieParseResult.Missing,
        SessionCookieParseResult.Invalid -> BrowserSession.MissingCookie
        is SessionCookieParseResult.Valid -> when (val lookup = registry.lookup(cookie.id)) {
            is SessionLookup.Found -> BrowserSession.Found(cookie.id, lookup.session)
            SessionLookup.Missing -> BrowserSession.MissingRegistrySession
        }
    }
}

internal suspend fun ApplicationCall.respondAuthenticationRequired() {
    respondFailure(
        this,
        HttpStatusCatalog.UNAUTHORIZED,
        ApiFailure.authenticationRequired(
            UiMessages.server_authenticationRequired(),
            ServerMessageKey.AUTHENTICATION_REQUIRED
        )
    )
}

internal suspend fun ApplicationCall.requireBrowserSession(
    message: String,
    messageKey: ServerMessageKey = ServerMessageKey.AUTHENTICATION_REQUIRED
): UpstreamSession? =
    authenticatedSessionOrNull()?.takeIf(UpstreamSession::hasAuthorizedAccount)
        ?.also { session ->
            if (authorizationContextOrNull() == null) attachAuthenticatedSession(session)
        }
        ?: (browserSession(this, this.sessionDependencies.sessions) as? BrowserSession.Found)?.session
            ?.takeIf(UpstreamSession::hasAuthorizedAccount)
            ?.also(::attachAuthenticatedSession) ?: run {
            respondFailure(this, HttpStatusCatalog.UNAUTHORIZED, ApiFailure(message, messageKey))
            null
}

