package f95gm.client.actions.catalog.pagination

import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun startCatalogPageEdit() {
    scope.launch {
        catalog.enqueue { current ->
            current.copy(
                editingPage = true,
                pageInput = current.page.toString()
            )
        }
    }
}
