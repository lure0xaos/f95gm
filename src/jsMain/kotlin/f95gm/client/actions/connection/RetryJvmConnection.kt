package f95gm.client.actions.connection

import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.ClientConnectionActionsPort

internal fun retryJvmConnection(runtime: ClientConnectionActionsPort) {
    runtime.requests.cancelJob(ClientJobKey.JVM_CONNECTION)
    startJvmConnectionMonitor(runtime)
}
