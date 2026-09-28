package f95gm.client.actions.catalog.filters.editing

import f95gm.client.model.catalog.FilterSelection
import f95gm.client.state.runtime.ClientCatalogActionsPort
import kotlinx.coroutines.launch

internal fun removePrefix(runtime: ClientCatalogActionsPort, id: String, selection: FilterSelection) {
    runtime.launchEffect {
        runtime.catalog.enqueue { state ->
            if (selection == FilterSelection.EXCLUDED) state.copy(excludedPrefixes = state.excludedPrefixes.filterNot { it.id.value == id })
            else state.copy(selectedPrefixes = state.selectedPrefixes.filterNot { it.id.value == id })
        }
    }
}
