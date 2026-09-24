package f95gm.client.actions.catalog.filters.editing

import f95gm.client.model.catalog.FilterSelection
import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun removePrefix(id: String, selection: FilterSelection) {
    scope.launch {
        catalog.enqueue { state ->
            if (selection == FilterSelection.EXCLUDED) state.copy(excludedPrefixes = state.excludedPrefixes.filterNot { it.id.value == id })
            else state.copy(selectedPrefixes = state.selectedPrefixes.filterNot { it.id.value == id })
        }
    }
}
