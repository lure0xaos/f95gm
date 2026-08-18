package f95gm.server.app

import f95gm.domain.api.F95Api
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.logging.logger
import f95gm.server.desktop.DesktopController
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.static.static
import f95gm.server.launch.openBrowser
import f95gm.server.persistence.database.initDatabase
import f95gm.server.persistence.sessions.restoreSessions
import f95gm.server.routes.catalog.catalog
import f95gm.server.routes.catalog.catalogOptions
import f95gm.server.routes.filters.*
import f95gm.server.routes.item.item
import f95gm.server.routes.item.media
import f95gm.server.routes.marks.checkedMarks
import f95gm.server.routes.marks.deleteMark
import f95gm.server.routes.marks.marks
import f95gm.server.routes.marks.saveMark
import f95gm.server.routes.session.login
import f95gm.server.routes.session.logout
import f95gm.server.routes.session.sessionStatus
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level
import java.net.URI
import java.util.concurrent.CountDownLatch
import kotlin.uuid.Uuid
import io.ktor.server.cio.CIO as ServerCIO

internal fun startServer() {
    DesktopController.installConsoleCapture()
    initDatabase()
    restoreSessions()
    val port = System.getProperty("f95gm.port", ServerConstants.DEFAULT_PORT.toString())
        .toIntOrNull()?.coerceIn(ServerConstants.MIN_PORT, ServerConstants.MAX_PORT) ?: ServerConstants.DEFAULT_PORT
    val host = System.getProperty("f95gm.host", ServerConstants.DEFAULT_HOST)
    val shutdownToken = Uuid.random().toString()
    lateinit var shutdownController: ServerShutdownController
    var desktop: DesktopController? = null
    val server = embeddedServer(
        factory = ServerCIO,
        host = host,
        port = port
    ) {
        install(CallLogging) {
            disableDefaultColors()
            level = Level.INFO
            filter { call -> call.request.uri.substringBefore('?') != F95Api.HEALTH }
            format { call ->
                val status = call.response.status()?.value ?: "-"
                "${call.request.httpMethod.value} ${call.request.uri} -> $status"
            }
        }
        routing {
            get(F95Api.HEALTH) {
                desktop?.updateTheme(call.request.queryParameters[F95Api.THEME])
                shutdownController.browserHeartbeat(call.request.queryParameters[F95Api.TAB_ID])
                call.response.headers.append(HttpHeaders.CacheControl, "no-store")
                call.respond(HttpStatusCode.OK)
            }
            post(F95Api.SHUTDOWN) {
                if (call.request.cookies[ServerConstants.SHUTDOWN_COOKIE] != shutdownToken) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    call.respond(HttpStatusCode.Accepted)
                    shutdownController.requestFromBrowser(call.request.queryParameters[F95Api.TAB_ID])
                }
            }
            post(F95Api.LOGIN) { login(call) }
            post(F95Api.LOGOUT) { logout(call) }
            get(F95Api.SESSION) { sessionStatus(call) }
            get(F95Api.CATALOG_OPTIONS) { catalogOptions(call) }
            get(F95Api.CATALOG) { catalog(call) }
            get(F95Api.MARKS) { marks(call) }
            get(F95Api.MARKS_CHECK) { checkedMarks(call) }
            put("${F95Api.MARKS}/{threadId}") { saveMark(call) }
            delete("${F95Api.MARKS}/{threadId}") { deleteMark(call) }
            get(F95Api.FILTERS) { filters(call) }
            get(F95Api.FILTERS_EXPORT) { exportFilters(call) }
            post(F95Api.FILTERS_IMPORT) { importFilters(call) }
            post(F95Api.FILTERS) { saveFilter(call) }
            delete("${F95Api.FILTERS}/{filterId}") { deleteFilter(call) }
            get("${F95Api.ITEM}/{threadId}") { item(call) }
            get(F95Api.MEDIA) { media(call) }
            get("/") {
                setShutdownCookie(call, shutdownToken)
                static(call, "index.html")
            }
            get("/{path...}") {
                setShutdownCookie(call, shutdownToken)
                val path = call.parameters.getAll("path")?.joinToString("/").orEmpty()
                val staticPath = when {
                    isItemDetailPath(path) -> "index.html"
                    path.startsWith("item/") -> path.removePrefix("item/")
                    else -> path
                }
                static(call, staticPath)
            }
        }
    }
    shutdownController = ServerShutdownController {
        server.stop(gracePeriodMillis = 1_000, timeoutMillis = 5_000)
    }
    val terminated = CountDownLatch(1)
    server.application.monitor.subscribe(ApplicationStopped) { terminated.countDown() }
    server.start(wait = false)

    val connector = runBlocking { server.engine.resolvedConnectors().single() }
    val address = URI("http", null, connector.host, connector.port, "/", null, null)
    logger.info { "F95GM server started at $address" }
    val desktopController = DesktopController(shutdownController::request)
    desktop = desktopController
    desktopController.start()
    openBrowser(address)
    try {
        terminated.await()
    } finally {
        desktopController.dispose()
    }
}
