package f95gm.client.state.runtime

import kotlinx.coroutines.Job

/** Starts a named replaceable operation and records it in the supplied client lifecycle. */
internal fun launchClientOperation(runtime: ClientOperationPort, key: ClientJobKey, block: suspend () -> Unit): Job =
    runtime.launchOperation(key, block)

internal fun ClientOperationPort.cancelOperation(key: ClientJobKey) = requests.cancelJob(key)

internal fun ClientOperationPort.isOperationActive(key: ClientJobKey): Boolean = requests.isJobActive(key)
