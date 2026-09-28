package f95gm.client.actions.catalog.pagination

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.state.runtime.launchCatalogOperation
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.domain.api.parseIntOrNull
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first

internal fun submitCatalogPageInput(runtime: ClientCatalogActionsPort) {
    runtime.launchCatalogOperation {
        val current = runtime.catalog.data.first()
        val nextPage = current.pageInput.parseIntOrNull()
            ?.coerceIn(F95Defaults.FIRST_PAGE, current.totalPages)
            ?: current.page
        val next = current.copy(
            page = nextPage,
            editingPage = PageEditMode.BROWSING,
            pageInput = F95Defaults.EMPTY
        )
        runtime.catalog.enqueue { next }
        if (nextPage != current.page) loadCatalog(runtime, next)
    }
}
