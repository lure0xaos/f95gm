package f95gm.client.actions.catalog.filters.editing

import f95gm.client.state.catalog.filterPanelOpen
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun toggleFilterPanel() {
    scope.launch {
        filterPanelOpen.enqueue { isOpen -> !isOpen }
    }
}
