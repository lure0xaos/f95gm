package f95gm.client.actions.dropdown

import f95gm.client.state.runtime.ClientDropdownActionsPort
import f95gm.client.state.dropdown.DropdownSearchKey
import kotlinx.coroutines.launch

internal fun updateDropdownSearch(runtime: ClientDropdownActionsPort, key: DropdownSearchKey, query: String) {
    runtime.launchEffect { runtime.updateDropdownSearch { current -> current.update(key, query) } }
}
