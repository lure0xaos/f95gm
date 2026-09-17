package f95gm.client.actions.catalog.filters.editing

import f95gm.client.state.runtime.ClientCatalogActionsPort
import kotlinx.coroutines.launch

internal fun toggleFilterPanel(runtime: ClientCatalogActionsPort) {
    runtime.launchEffect {
        runtime.filterPanelOpen.enqueue { isOpen -> !isOpen }
    }
}
