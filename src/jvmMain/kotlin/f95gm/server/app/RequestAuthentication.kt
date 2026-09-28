package f95gm.server.app

import f95gm.domain.api.AuthRequirement
import f95gm.domain.api.EndpointDescriptor
import f95gm.server.routes.session.attachAuthenticatedSession
import f95gm.server.routes.session.browserSession
import f95gm.server.routes.session.BrowserSession
import f95gm.server.routes.session.hasAuthorizedAccount
import f95gm.server.routes.session.respondAuthenticationRequired
import f95gm.server.application.SessionRegistry
import f95gm.server.observability.*
import io.ktor.server.application.ApplicationCall

/** Owns session authentication decisions so request monitoring remains orchestration-only. */
internal suspend fun ApplicationCall.authenticateIfRequired(
    descriptor: EndpointDescriptor?,
    sessions: SessionRegistry
): AuthenticationResult {
    if (descriptor?.auth != AuthRequirement.SESSION) return AuthenticationResult.Allowed
    val session = (browserSession(this, sessions) as? BrowserSession.Found)?.session
    if (session == null || !session.hasAuthorizedAccount()) {
        logger.info {
            auditLog(
                AuditEvent(
                    AuditEventName.AUTHORIZATION_REJECTED,
                    listOf(logField(StructuredLogKeys.REASON, "missing_or_unauthorized_session"), logField(StructuredLogKeys.OUTCOME, "rejected"))
                )
            )
        }
        respondAuthenticationRequired()
        return AuthenticationResult.ResponseCompleted
    }
    attachAuthenticatedSession(session)
    return AuthenticationResult.Allowed
}
