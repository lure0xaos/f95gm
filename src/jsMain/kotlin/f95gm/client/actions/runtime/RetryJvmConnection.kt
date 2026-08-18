package f95gm.client.actions.runtime

import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun retryJvmConnection() {
    scope.launch { checkJvmConnection() }
}
