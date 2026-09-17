package f95gm.server.app

import f95gm.server.config.ServerConfigSnapshot
import f95gm.server.platform.SystemConfigurationSource
import f95gm.server.observability.ConfigurationLoadedEvent
import f95gm.server.observability.logger
import f95gm.server.observability.structuredLog
import f95gm.server.observability.StructuredLogSink
import f95gm.server.observability.StructuredLogSinks
import f95gm.server.http.request.RequestIdentifierFactory
import f95gm.server.state.runtime.ServerApplicationComposition
import f95gm.server.state.runtime.StartupMetrics
import f95gm.server.state.runtime.StartupRollbackStack
import f95gm.server.state.runtime.StartupState
import f95gm.server.state.runtime.StartupStage

/** Owns the ordered startup phases while the caller retains failure and rollback policy. */
internal class ServerStartupCoordinator(
    private val identifiers: RequestIdentifierFactory,
    private val startup: StartupState,
    private val lifecycleMetrics: StartupMetrics,
    private val rollback: StartupRollbackStack,
    private val healthState: ServerHealthState,
    private val structuredLogSink: StructuredLogSink = StructuredLogSinks.text
) {
    suspend fun start(): ServerStartupResult {
        val config = ServerConfigSnapshot.fromSystemProperties(SystemConfigurationSource).value
        startup.advance(StartupStage.CONFIGURED)
        logger.info {
            structuredLog(
                ConfigurationLoadedEvent(
                    host = config.host,
                    port = config.port.value,
                    database = config.database,
                    staticDirectoryConfigured = config.staticDirectory != null,
                    deploymentMode = config.deploymentMode.name,
                    openBrowser = config.openBrowser
                ),
                structuredLogSink
            )
        }

        val compositionHandle = ServerCompositionRoot.handle(config)
        startup.advance(StartupStage.DATABASE_READY)
        rollback.register("composition", compositionHandle)
        startup.advance(StartupStage.COMPOSED)
        val composition = compositionHandle.composition

        val runtime = ServerRuntimeState(lifecycleMetrics)
        rollback.register("runtime", runtime)
        val startupStartedAt = composition.runtime.clock.monotonicTimestamp()
        composition.persistence.sessionService.restore()
        return ServerRuntimeBootstrap(identifiers, startup, healthState, structuredLogSink = structuredLogSink)
            .start(config, composition, runtime, startupStartedAt)
    }
}
