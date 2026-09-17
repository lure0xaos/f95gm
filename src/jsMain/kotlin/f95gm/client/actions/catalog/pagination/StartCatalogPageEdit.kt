package f95gm.client.actions.catalog.pagination

import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.ui.shared.pagination.PageEditMode
import kotlinx.coroutines.launch

internal fun startCatalogPageEdit(runtime: ClientCatalogActionsPort) {
    runtime.launchEffect {
        runtime.catalog.enqueue { current ->
            current.copy(
                editingPage = PageEditMode.EDITING,
                pageInput = current.page.toString()
            )
        }
    }
}
