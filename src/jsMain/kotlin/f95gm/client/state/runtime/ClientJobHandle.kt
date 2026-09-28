package f95gm.client.state.runtime

internal value class ClientJobHandle(val key: ClientJobKey)

internal fun ClientController.cancel(handle: ClientJobHandle) = cancelJob(handle.key)
