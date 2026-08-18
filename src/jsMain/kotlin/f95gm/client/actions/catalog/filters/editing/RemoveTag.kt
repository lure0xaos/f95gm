package f95gm.client.actions.catalog.filters.editing

import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun removeTag(id: String, excluded: Boolean) {
    scope.launch {
        catalog.enqueue { state ->
            if (excluded) state.copy(excludedTags = state.excludedTags.filterNot { it.id.value == id })
            else state.copy(selectedTags = state.selectedTags.filterNot { it.id.value == id })
        }
    }
}
