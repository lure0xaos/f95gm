package f95gm.client.state.runtime

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Starts a named replaceable operation and records it in the shared client lifecycle. */
internal fun launchClientOperation(key: ClientJobKey, block: suspend () -> Unit): Job =
    clientController.replaceJob(key, scope.launch { block() })

internal fun clientOperation(key: ClientJobKey): Job? = clientController.currentJob(key)
