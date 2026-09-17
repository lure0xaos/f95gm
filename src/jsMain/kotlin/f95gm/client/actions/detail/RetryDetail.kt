package f95gm.client.actions.detail

import f95gm.client.state.runtime.ClientDetailActionsPort
import f95gm.client.state.runtime.launchDetailOperation
import f95gm.domain.values.F95ThreadId

internal fun retryDetail(runtime: ClientDetailActionsPort, threadId: F95ThreadId) {
    runtime.launchDetailOperation { loadDetail(runtime, threadId) }
}
