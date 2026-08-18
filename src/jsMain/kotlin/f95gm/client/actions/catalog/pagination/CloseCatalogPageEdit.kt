package f95gm.client.actions.catalog.pagination

import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.launch

internal fun closeCatalogPageEdit() {
    scope.launch {
        catalog.enqueue {
            it.copy(
                editingPage = false,
                pageInput = F95Defaults.EMPTY
            )
        }
    }
}
