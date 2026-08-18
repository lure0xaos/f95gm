package f95gm.client.actions.catalog.pagination

import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun updateCatalogPageInput(input: String) {
    scope.launch {
        catalog.enqueue { it.copy(pageInput = input) }
    }
}
