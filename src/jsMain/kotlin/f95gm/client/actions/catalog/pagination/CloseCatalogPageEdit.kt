package f95gm.client.actions.catalog.pagination

import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.launch

internal fun closeCatalogPageEdit(runtime: ClientCatalogActionsPort) {
    runtime.launchEffect {
        runtime.catalog.enqueue {
            it.copy(
                editingPage = PageEditMode.BROWSING,
                pageInput = F95Defaults.EMPTY
            )
        }
    }
}
