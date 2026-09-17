package f95gm.server.routes.session

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.model.auth.UpstreamSession
import io.ktor.server.application.*
import io.ktor.util.*

private val authenticatedSessionKey = AttributeKey<UpstreamSession>("f95gm.authenticatedSession")
private val authenticatedPrincipalKey = AttributeKey<AuthenticatedPrincipal>("f95gm.authenticatedPrincipal")

internal data class AuthenticatedPrincipal(
    val sessionId: F95SessionId,
    val accountKey: F95AccountKey
)

/** Request-scoped authorization data used by application services and audit logs. */
internal data class AuthorizationContext(
    val principal: AuthenticatedPrincipal,
    val session: UpstreamSession
)

internal fun ApplicationCall.authenticatedSessionOrNull(): UpstreamSession? =
    attributes.getOrNull(authenticatedSessionKey)

internal fun ApplicationCall.attachAuthenticatedSession(session: UpstreamSession) {
    attributes.put(authenticatedSessionKey, session)
    attributes.put(authenticatedPrincipalKey, AuthenticatedPrincipal(session.browserId, session.accountKey))
}

internal fun ApplicationCall.authenticatedPrincipalOrNull(): AuthenticatedPrincipal? =
    attributes.getOrNull(authenticatedPrincipalKey)

internal fun ApplicationCall.authorizationContextOrNull(): AuthorizationContext? =
    authenticatedPrincipalOrNull()?.let { principal ->
        authenticatedSessionOrNull()?.let { session -> AuthorizationContext(principal, session) }
    }

internal fun UpstreamSession.hasAuthorizedAccount(): Boolean =
    browserId.value.isNotBlank() && accountKey.value.isNotBlank()
