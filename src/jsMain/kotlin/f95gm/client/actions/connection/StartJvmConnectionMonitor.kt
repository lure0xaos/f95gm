package f95gm.client.actions.connection

import f95gm.client.state.config.ClientConstants
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.ClientConnectionActionsPort
import f95gm.client.state.runtime.launchJvmConnectionOperation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

internal fun startJvmConnectionMonitor(runtime: ClientConnectionActionsPort) {
    if (runtime.requests.isJobActive(ClientJobKey.JVM_CONNECTION)) return
    runtime.launchJvmConnectionOperation {
        while (currentCoroutineContext().isActive) {
            checkJvmConnection(runtime)
            runtime.clock.delay(ClientConstants.JVM_HEALTH_CHECK_INTERVAL_MILLIS.milliseconds)
        }
    }
}
