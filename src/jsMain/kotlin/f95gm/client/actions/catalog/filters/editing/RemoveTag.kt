package f95gm.client.actions.catalog.filters.editing

import f95gm.client.model.catalog.FilterSelection
import f95gm.client.state.runtime.ClientCatalogActionsPort
import kotlinx.coroutines.launch

internal fun removeTag(runtime: ClientCatalogActionsPort, id: String, selection: FilterSelection) {
    runtime.launchEffect {
        runtime.catalog.enqueue { state ->
            if (selection == FilterSelection.EXCLUDED) state.copy(excludedTags = state.excludedTags.filterNot { it.id.value == id })
            else state.copy(selectedTags = state.selectedTags.filterNot { it.id.value == id })
        }
    }
}
