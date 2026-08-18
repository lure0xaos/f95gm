package f95gm.client.actions.runtime

import f95gm.client.state.config.ClientConstants
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal fun startJvmConnectionMonitor() {
    if (jvmConnectionMonitorStarted) return
    jvmConnectionMonitorStarted = true
    scope.launch {
        while (isActive) {
            checkJvmConnection()
            delay(ClientConstants.JVM_HEALTH_CHECK_INTERVAL_MILLIS.milliseconds)
        }
    }
}
