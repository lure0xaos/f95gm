package f95gm.server.desktop

import f95gm.server.kernel.FailureBoundary
import f95gm.server.kernel.LifecycleResourceName
import f95gm.server.state.runtime.ActiveRequestTracker
import f95gm.server.state.runtime.runSuspendCleanupBoundary
import f95gm.server.state.runtime.LifecycleFailurePolicy
import kotlin.time.Duration

/** Encapsulates request draining and graceful-stop failure policy. */
internal class ServerShutdownDrain(
    private val activeRequests: ActiveRequestTracker?,
    private val timeout: Duration,
    private val stop: () -> Unit
) {
    suspend fun execute() {
        runSuspendCleanupBoundary(
            onFailure = { error ->
                LifecycleFailurePolicy.report(
                    LifecycleResourceName("server-shutdown"),
                    FailureBoundary.LIFECYCLE,
                    error,
                    "Could not stop the server gracefully"
                )
            }
        ) {
            activeRequests?.awaitZero(timeout)
            stop()
        }
    }
}
