package f95gm.server.routes.session

import f95gm.domain.values.F95SessionId
import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.session.parseSessionCookie
import f95gm.server.http.response.ApiFailure
import f95gm.server.http.response.ServerMessageKey
import f95gm.server.http.response.ServerMessageKeys
import f95gm.server.http.response.respondFailure
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.state.runtime.routeDependencies
import f95gm.server.state.runtime.session.SessionRegistry
import io.ktor.http.*
import io.ktor.server.application.*

internal fun browserSession(call: ApplicationCall, registry: SessionRegistry): Pair<F95SessionId, UpstreamSession>? {
    val id = parseSessionCookie(call.request.headers[HttpHeaders.Cookie])
    return id?.let { registry.get(it)?.let { stored -> it to stored } }
}

internal suspend fun ApplicationCall.respondAuthenticationRequired() {
    respondFailure(
        this,
        ServerConstants.STATUS_UNAUTHORIZED,
        ApiFailure.authenticationRequired(
            UiMessages.server_authenticationRequired(),
            ServerMessageKeys.AUTHENTICATION_REQUIRED
        )
    )
}

internal suspend fun ApplicationCall.requireBrowserSession(
    message: String,
    messageKey: ServerMessageKey = ServerMessageKeys.AUTHENTICATION_REQUIRED
): UpstreamSession? =
    authenticatedSessionOrNull()?.takeIf(UpstreamSession::hasAuthorizedAccount)
        ?.also { session ->
            if (authorizationContextOrNull() == null) attachAuthenticatedSession(session)
        }
        ?: browserSession(this, this.routeDependencies.sessions)?.second
            ?.takeIf(UpstreamSession::hasAuthorizedAccount)
            ?.also(::attachAuthenticatedSession) ?: run {
            respondFailure(this, ServerConstants.STATUS_UNAUTHORIZED, ApiFailure(message, messageKey))
            null
}
