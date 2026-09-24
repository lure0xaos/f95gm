package f95gm.server.app

import f95gm.server.http.request.ShutdownToken
import f95gm.server.http.static.StaticResourceCache
import f95gm.server.http.static.static
import io.ktor.server.routing.*

internal fun Route.configureStaticRoutes(shutdownToken: ShutdownToken, staticCache: StaticResourceCache) {
    get("/") {
        setShutdownCookie(call, shutdownToken)
        static(call, "index.html", staticCache)
    }
    get("/{path...}") {
        setShutdownCookie(call, shutdownToken)
        val path = call.parameters.getAll("path")?.joinToString("/").orEmpty()
        val staticPath = when {
            isItemDetailPath(path) -> "index.html"
            path.startsWith("item/") -> path.removePrefix("item/")
            else -> path
        }
        static(call, staticPath, staticCache)
    }
}
