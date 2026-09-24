package f95gm.server.app

import f95gm.domain.api.F95Api
import f95gm.server.config.constants.ServerConstants
import f95gm.server.desktop.DesktopController
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.response.respondJson
import f95gm.server.state.runtime.ServerContext
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Route.configureHealthRoute(
    shutdownController: () -> ServerShutdownController,
    desktop: () -> DesktopController?,
    context: ServerContext
) {
    fun Route.live(path: String) = get(path) {
        desktop()?.updateTheme(call.request.queryParameters[F95Api.THEME])
        shutdownController().browserHeartbeat(call.request.queryParameters[F95Api.TAB_ID])
        call.response.headers.append(HttpHeaders.CacheControl, "no-store")
        call.respond(ServerConstants.STATUS_OK)
    }
    live(F95Api.HEALTH)
    live(F95Api.LIVE)
    get(F95Api.READY) {
        call.response.headers.append(HttpHeaders.CacheControl, "no-store")
        if (shutdownController().isDraining()) call.respond(HttpStatusCode.ServiceUnavailable)
        else call.respond(ServerConstants.STATUS_OK)
    }
    get(F95Api.METRICS) {
        call.response.headers.append(HttpHeaders.CacheControl, "no-store")
        respondJson(call, ServerConstants.STATUS_OK, context.requestMetrics.snapshot())
    }
}
