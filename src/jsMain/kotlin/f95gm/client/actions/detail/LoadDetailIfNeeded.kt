package f95gm.client.actions.detail

import f95gm.client.state.runtime.ClientDetailActionsPort
import f95gm.client.state.runtime.launchDetailOperation
import f95gm.domain.values.F95ThreadId

internal fun loadDetailIfNeeded(runtime: ClientDetailActionsPort, threadId: F95ThreadId) {
    if (runtime.detailLoading.loadedDetailId == threadId) return
    runtime.detailLoading.loadedDetailId = threadId
    runtime.launchDetailOperation { loadDetail(runtime, threadId) }
}
