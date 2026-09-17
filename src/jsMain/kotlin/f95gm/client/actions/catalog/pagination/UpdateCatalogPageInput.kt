package f95gm.client.actions.catalog.pagination

import f95gm.client.state.runtime.ClientCatalogActionsPort
import kotlinx.coroutines.launch

internal fun updateCatalogPageInput(runtime: ClientCatalogActionsPort, input: String) {
    runtime.launchEffect {
        runtime.catalog.enqueue { it.copy(pageInput = input) }
    }
}
