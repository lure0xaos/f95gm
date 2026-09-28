package f95gm.server.app

import f95gm.server.observability.*
import f95gm.server.desktop.DesktopController
import f95gm.server.http.request.RequestIdentifierFactory
import f95gm.server.state.runtime.failureCategory
import kotlinx.coroutines.CancellationException

@Suppress("TooGenericExceptionCaught") // Startup boundary converts every ordinary failure after rollback.
internal suspend fun startServer(
    identifiers: RequestIdentifierFactory = RequestIdentifierFactory(),
    structuredLogSink: StructuredLogSink = StructuredLogSinks.text
) {
    val startup = f95gm.server.state.runtime.StartupState()
    val lifecycleMetrics = f95gm.server.state.runtime.StartupMetrics()
    val rollback = f95gm.server.state.runtime.StartupRollbackStack(lifecycleMetrics)
    val healthState = ServerHealthState()
    val consoleCapture = DesktopController.installConsoleCapture()
    rollback.register("console-capture", consoleCapture)
    var startupResult: ServerStartupResult? = null
    var startupFailure: Throwable? = null
    try {
        startupResult = ServerStartupCoordinator(identifiers, startup, lifecycleMetrics, rollback, healthState, structuredLogSink).start()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        healthState.markFailed()
        val typedFailure = f95gm.server.state.runtime.StartupFailure(
            category = startup.current().failureCategory(),
            stage = startup.current(),
            cause = failure
        )
        startup.fail()
        startupFailure = failure
        failure.addSuppressed(typedFailure)
        throw failure
    } finally {
        healthState.markStopping()
        if (startup.current() != f95gm.server.state.runtime.StartupStage.FAILED) startup.stop()
        startupResult?.let { result ->
            val composition = result.composition
            val metrics = composition.runtime.requestMetrics.snapshot()
            val activeRequests = composition.runtime.activeRequests.activeCount()
            logger.info {
                structuredLog(
                    ServerStoppedEvent(
                        uptimeMillis = composition.runtime.clock.elapsedSince(result.startupStartedAt).inWholeMilliseconds
                            .coerceAtLeast(0L),
                        activeRequests = activeRequests,
                        requests = metrics.requests,
                        rejected = metrics.rejected,
                        retries = metrics.retries,
                        cacheEvictions = metrics.evictions,
                        circuitOpenings = metrics.circuitOpenings
                    ), structuredLogSink
                )
            }
            logger.info {
                structuredLog(
                    LifecycleMetricsEvent(
                        rollbackFailures = lifecycleMetrics.rollbackFailures.get(),
                        lifecycleRejections = lifecycleMetrics.lifecycleRejections.get(),
                        configurationReloadRejections = composition.runtime.configurationState.rejectedReloadCount
                    ),
                    structuredLogSink
                )
            }
        }
        // The rollback stack owns constructed resources for both failed and normal shutdown paths.
        val rollbackFailures = rollback.rollback()
        startupFailure?.let { failure -> rollbackFailures.forEach(failure::addSuppressed) }
        healthState.markStopped()
    }
}
