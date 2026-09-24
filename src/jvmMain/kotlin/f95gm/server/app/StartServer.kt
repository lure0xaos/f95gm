package f95gm.server.app

import f95gm.domain.api.F95Api
import f95gm.server.config.ServerConfigSnapshot
import f95gm.server.config.logging.*
import f95gm.server.desktop.DesktopController
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.logging.redactLogValue
import f95gm.server.http.request.RequestIdentifierFactory
import f95gm.server.http.request.ratelimit.RateLimitPolicy
import f95gm.server.launch.openBrowser
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.database.initDatabase
import f95gm.server.state.runtime.ServerContext
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.Level
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import io.ktor.server.cio.CIO as ServerCIO

internal suspend fun startServer(identifiers: RequestIdentifierFactory = RequestIdentifierFactory()) {
    DesktopController.installConsoleCapture()
    val config = ServerConfigSnapshot.fromSystemProperties().value
    logger.info { structuredLog(ConfigurationLoadedEvent(config.redactedSummary())) }
    val database = initDatabase(config)
    val context = ServerContext(config, databaseExecutor = DatabaseExecutor(database = { database }))
    val runtime = ServerRuntimeState()
    val startupStartedAt = context.clock.monotonicNanos()
    try {
        context.sessionService.restore()
        val shutdownToken = identifiers.shutdownTokens.next()
        val shutdownController = runtime.createShutdownController(context)
        val server = embeddedServer(
            factory = ServerCIO,
            host = config.host,
            port = config.port
        ) {
            configureStatusPages()
            install(CallLogging) {
                disableDefaultColors()
                level = Level.INFO
                filter { call -> call.request.uri.substringBefore('?') != F95Api.HEALTH }
                format { call ->
                    val status = call.response.status()?.value ?: "-"
                    "${call.request.httpMethod.value} ${redactLogValue(call.request.uri.substringBefore('?'))} -> $status"
                }
            }
            configureRoutes(
                shutdownToken,
                { shutdownController },
                runtime::desktopController,
                RouteConfiguration(
                    staticDirectory = config.staticDirectory,
                    maxRequestBodyBytes = config.limits.maxRequestBodyBytes,
                    rateLimitPolicy = RateLimitPolicy.configured(
                        config.traffic.ipRequestsPerMinute,
                        config.traffic.accountRequestsPerMinute,
                        config.traffic.rateLimitWindowMillis.value
                    ),
                    maxRateLimitBuckets = RateLimitBucketLimit(config.traffic.maxRateLimitBuckets),
                    requestIdGenerator = identifiers.requestIds
                ),
                context,
            )
        }
        runtime.bind(server.engine)
        val terminated = CountDownLatch(1)
        server.application.monitor.subscribe(ApplicationStopped) { terminated.countDown() }
        server.start(wait = false)

        val connector = server.engine.resolvedConnectors().single()
        val address = URI("http", null, connector.host, connector.port, "/", null, null)
        logger.info {
            structuredLog(
                ServerStartedEvent(
                    address = address,
                    host = config.host,
                    port = connector.port,
                    startupMillis = (context.clock.monotonicNanos() - startupStartedAt).coerceAtLeast(0L) / 1_000_000L
                )
            )
        }
        val desktop = DesktopController(shutdownController::request)
        runtime.bind(desktop)
        desktop.start()
        if (config.openBrowser) openBrowser(address)
        terminated.await()
    } finally {
        val metrics = context.requestMetrics.snapshot()
        val activeRequests = context.activeRequests.activeCount()
        logger.info {
            structuredLog(
                ServerStoppedEvent(
                    uptimeMillis = (context.clock.monotonicNanos() - startupStartedAt).coerceAtLeast(0L) / 1_000_000L,
                    activeRequests = activeRequests,
                    requests = metrics.requests,
                    rejected = metrics.rejected,
                    retries = metrics.retries,
                    cacheEvictions = metrics.evictions,
                    circuitOpenings = metrics.circuitOpenings
                )
            )
        }
        runtime.close()
        context.close()
    }
}

private class ServerRuntimeState {
    private val server = AtomicReference<ApplicationEngine?>()
    private val desktop = AtomicReference<DesktopController?>()
    private val shutdownController = AtomicReference<ServerShutdownController?>()

    fun createShutdownController(context: ServerContext): ServerShutdownController =
        ServerShutdownController({
            server.get()?.stop(gracePeriodMillis = 1_000, timeoutMillis = 5_000)
                ?: error("Server engine is not initialized")
        }, context.clock, context.activeRequests).also(shutdownController::set)

    fun bind(server: ApplicationEngine) {
        this.server.set(server)
    }

    fun bind(desktop: DesktopController) {
        this.desktop.set(desktop)
    }

    fun desktopController(): DesktopController? = desktop.get()

    fun close() {
        desktop.getAndSet(null)?.dispose()
        shutdownController.getAndSet(null)?.close()
        server.set(null)
    }
}
