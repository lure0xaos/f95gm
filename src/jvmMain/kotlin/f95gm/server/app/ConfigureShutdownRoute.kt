package f95gm.server.app

import f95gm.domain.api.F95ApiPaths
import f95gm.domain.api.F95ApiQueryParameters
import f95gm.server.config.protocol.SessionProtocolConfig
import f95gm.server.config.constants.HttpStatusCatalog
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.request.ShutdownToken
import f95gm.server.http.request.get
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Route.configureShutdownRoute(
    shutdownToken: ShutdownToken,
    shutdownController: () -> ServerShutdownController
) {
    post(F95ApiPaths.SHUTDOWN) {
        if (call.request.cookies[SessionProtocolConfig.SHUTDOWN_COOKIE] != shutdownToken.value) {
            call.respond(HttpStatusCatalog.FORBIDDEN)
        } else {
            call.respond(HttpStatusCatalog.ACCEPTED)
            shutdownController().requestFromBrowser(call.request.queryParameters[F95ApiQueryParameters.TAB_ID])
        }
    }
}
