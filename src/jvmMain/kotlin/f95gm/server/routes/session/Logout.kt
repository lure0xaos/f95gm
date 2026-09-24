package f95gm.server.routes.session

import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.logging.*
import f95gm.server.http.request.currentRequestContext
import f95gm.server.http.response.respondJson
import f95gm.server.model.http.OkResponse
import f95gm.server.state.runtime.routeDependencies
import io.ktor.server.application.*

internal suspend fun logout(call: ApplicationCall) {
    browserSession(call, call.routeDependencies.sessions)?.let {
        call.routeDependencies.sessionService.removeInMemory(it.first)
        call.routeDependencies.sessionService.remove(it.first)
    }
    clearBrowserCookie(call)
    logger.info {
        auditLog(
            AuditEvent(
                AuditEventName.LOGOUT, listOf(
                    logField("requestId", currentRequestContext()?.requestId?.value),
                    logField(
                        "account",
                        browserSession(call, call.routeDependencies.sessions)?.second?.accountKey?.value
                    ),
                    logField("outcome", "success")
                )
            )
        )
    }
    respondJson(call, ServerConstants.STATUS_OK, OkResponse(true))
}
