package f95gm.server.app

import f95gm.domain.api.F95ApiPaths
import f95gm.server.config.constants.HttpStatusCatalog
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.response.respondJson
import f95gm.server.http.request.RequestMetrics
import f95gm.server.http.request.toHttpResponse
import f95gm.server.cache.CacheStatisticsProvider
import f95gm.server.http.response.appendNoStore
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Route.configureHealthRoute(
    shutdownController: () -> ServerShutdownController,
    requestMetrics: RequestMetrics,
    cacheStatisticsProvider: CacheStatisticsProvider,
    healthState: ServerHealthState
) {
    fun Route.live(path: String) = get(path) {
        call.appendNoStore()
        if (healthState.isLive) call.respond(HttpStatusCatalog.OK)
        else call.respond(HttpStatusCode.ServiceUnavailable)
    }
    live(F95ApiPaths.HEALTH)
    live(F95ApiPaths.LIVE)
    get(F95ApiPaths.READY) {
        call.appendNoStore()
        if (!healthState.isReady || shutdownController().isDraining) call.respond(HttpStatusCode.ServiceUnavailable)
        else call.respond(HttpStatusCatalog.OK)
    }
    get(F95ApiPaths.METRICS) {
        call.appendNoStore()
        respondJson(call, HttpStatusCatalog.OK, requestMetrics.snapshot(cacheStatisticsProvider).toHttpResponse())
    }
}
