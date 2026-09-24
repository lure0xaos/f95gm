package f95gm.client.actions.detail

import f95gm.client.state.runtime.scope
import f95gm.domain.values.F95ThreadId
import kotlinx.coroutines.launch

internal fun retryDetail(threadId: F95ThreadId) {
    scope.launch { loadDetail(threadId) }
}
