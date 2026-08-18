package f95gm.client.ui.catalog.search

import f95gm.client.state.dropdown.dropdownSearches
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun updateDropdownSearch(key: String, query: String) {
    scope.launch { dropdownSearches.enqueue { current -> current + (key to query) } }
}
