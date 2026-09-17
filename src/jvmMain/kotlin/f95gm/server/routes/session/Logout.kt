package f95gm.server.routes.session

import f95gm.server.observability.*
import f95gm.server.http.request.currentRequestIdValue
import f95gm.server.http.response.respondJson
import f95gm.server.model.http.OkResponse
import f95gm.server.routes.session.sessionDependencies
import io.ktor.server.application.*
import f95gm.server.config.constants.HttpStatusCatalog

internal suspend fun logout(call: ApplicationCall) {
    val current = browserSession(call, call.sessionDependencies.sessions)
    if (current is BrowserSession.Found) {
        call.sessionDependencies.sessionService.removeInMemory(current.id)
        call.sessionDependencies.sessionService.remove(current.id)
    }
    clearBrowserCookie(call)
    logger.info {
        auditLog(
            AuditEvent(
                AuditEventName.LOGOUT, listOf(
                    logField(StructuredLogKeys.REQUEST_ID, currentRequestIdValue()),
                    logField(
                        StructuredLogKeys.ACCOUNT,
                        redactLogValue(
                            "accountKey=${(current as? BrowserSession.Found)?.session?.accountKey?.value ?: "-"}"
                        )
                    ),
                    logField(StructuredLogKeys.OUTCOME, AuditOutcome.SUCCESS.wireName)
                )
            )
        )
    }
    respondJson(call, HttpStatusCatalog.OK, OkResponse(true))
}
