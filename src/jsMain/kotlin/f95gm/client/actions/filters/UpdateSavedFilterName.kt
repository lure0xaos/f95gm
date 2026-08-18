package f95gm.client.actions.filters

import f95gm.client.state.filters.savedFilterName
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun updateSavedFilterName(name: String) {
    scope.launch { savedFilterName.enqueue { name } }
}
