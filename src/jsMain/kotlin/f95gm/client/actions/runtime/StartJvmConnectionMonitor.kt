package f95gm.client.actions.runtime

import f95gm.client.state.config.ClientConstants
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.clientController
import f95gm.client.state.runtime.launchClientOperation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

internal fun startJvmConnectionMonitor() {
    if (clientController.isJobActive(ClientJobKey.JVM_CONNECTION)) return
    launchClientOperation(ClientJobKey.JVM_CONNECTION) {
        while (currentCoroutineContext().isActive) {
            checkJvmConnection()
            delay(ClientConstants.JVM_HEALTH_CHECK_INTERVAL_MILLIS.milliseconds)
        }
    }
}
