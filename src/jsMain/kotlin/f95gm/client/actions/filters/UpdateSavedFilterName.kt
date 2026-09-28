package f95gm.client.actions.filters

import f95gm.client.state.runtime.ClientFiltersActionsPort
import kotlinx.coroutines.launch

internal fun updateSavedFilterName(runtime: ClientFiltersActionsPort, name: String) {
    runtime.launchEffect { runtime.savedFilterName.enqueue { name } }
}
