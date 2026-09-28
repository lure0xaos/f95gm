package f95gm.server.app

import f95gm.server.config.ServerConfig
import f95gm.server.kernel.MonotonicNanos
import f95gm.server.observability.ServerStartedEvent
import f95gm.server.observability.logger
import f95gm.server.observability.structuredLog
import f95gm.server.observability.StructuredLogSink
import f95gm.server.observability.StructuredLogSinks
import f95gm.server.desktop.DesktopController
import f95gm.server.desktop.DesktopLifecycle
import f95gm.server.http.request.RequestIdentifierFactory
import f95gm.server.http.request.ShutdownToken
import f95gm.server.launch.openBrowser
import f95gm.server.state.runtime.ServerApplicationComposition
import f95gm.server.state.runtime.StartupState
import f95gm.server.state.runtime.StartupStage
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.EmbeddedServer
import kotlinx.coroutines.CompletableDeferred
import java.net.URI

internal data class ServerStartupResult(
    val composition: ServerApplicationComposition,
    val startupStartedAt: MonotonicNanos
)

internal fun interface ServerEngineFactory {
    fun create(
        config: ServerConfig,
        shutdownToken: ShutdownToken,
        shutdownController: f95gm.server.desktop.ServerShutdownController,
        composition: ServerApplicationComposition,
        identifiers: RequestIdentifierFactory,
        healthState: ServerHealthState
    ): EmbeddedServer<*, *>
}

internal fun interface DesktopLifecycleFactory {
    fun create(requestShutdown: () -> Unit): DesktopLifecycle
}

/** Binds and serves the runtime after the application graph has been composed. */
internal class ServerRuntimeBootstrap(
    private val identifiers: RequestIdentifierFactory,
    private val startup: StartupState,
    private val healthState: ServerHealthState,
    private val engineFactory: ServerEngineFactory = ServerEngineFactory(::createServerEngine),
    private val desktopFactory: DesktopLifecycleFactory = DesktopLifecycleFactory(::DesktopController),
    private val browserLauncher: (URI) -> Unit = ::openBrowser,
    private val structuredLogSink: StructuredLogSink = StructuredLogSinks.text
) {
    suspend fun start(
        config: ServerConfig,
        composition: ServerApplicationComposition,
        runtime: ServerRuntimeState,
        startupStartedAt: MonotonicNanos
    ): ServerStartupResult {
        val shutdownToken = identifiers.shutdownTokens.next()
        val shutdownController = runtime.createShutdownController(composition)
        val server = engineFactory.create(config, shutdownToken, shutdownController, composition, identifiers, healthState)
        runtime.bind(server.engine)
        val terminated = CompletableDeferred<Unit>()
        server.application.monitor.subscribe(ApplicationStopped) { terminated.complete(Unit) }
        server.start(wait = false)
        startup.advance(StartupStage.SERVING)
        healthState.markServing()

        val connector = server.engine.resolvedConnectors().single()
        val address = URI("http", null, connector.host, connector.port, "/", null, null)
        logger.info {
            structuredLog(
                ServerStartedEvent(
                    address = address.toString(),
                    host = config.serverHost.value,
                    port = connector.port,
                    startupMillis = composition.runtime.clock.elapsedSince(startupStartedAt).inWholeMilliseconds
                ), structuredLogSink
            )
        }
        val desktop = desktopFactory.create(shutdownController::request)
        runtime.bind(desktop)
        desktop.start()
        if (config.openBrowser) browserLauncher(address)
        terminated.await()
        return ServerStartupResult(composition, startupStartedAt)
    }
}
