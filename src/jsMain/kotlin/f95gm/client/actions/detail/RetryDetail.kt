package f95gm.client.actions.detail

import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun retryDetail(threadId: String) {
    scope.launch { loadDetail(threadId) }
}
