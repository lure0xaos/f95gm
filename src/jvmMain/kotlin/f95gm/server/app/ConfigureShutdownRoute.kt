package f95gm.server.app

import f95gm.domain.api.F95Api
import f95gm.server.config.constants.ServerConstants
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.request.ShutdownToken
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Route.configureShutdownRoute(
    shutdownToken: ShutdownToken,
    shutdownController: () -> ServerShutdownController
) {
    post(F95Api.SHUTDOWN) {
        if (call.request.cookies[ServerConstants.SHUTDOWN_COOKIE] != shutdownToken.value) {
            call.respond(ServerConstants.STATUS_FORBIDDEN)
        } else {
            call.respond(ServerConstants.STATUS_ACCEPTED)
            shutdownController().requestFromBrowser(call.request.queryParameters[F95Api.TAB_ID])
        }
    }
}
