package f95gm.client.actions.detail

import f95gm.client.state.detail.loadedDetailId
import f95gm.client.state.runtime.scope
import f95gm.domain.values.F95ThreadId
import kotlinx.coroutines.launch

internal fun loadDetailIfNeeded(threadId: F95ThreadId) {
    if (loadedDetailId == threadId) return
    loadedDetailId = threadId
    scope.launch { loadDetail(threadId) }
}
