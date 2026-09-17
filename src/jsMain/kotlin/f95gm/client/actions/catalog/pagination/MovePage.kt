package f95gm.client.actions.catalog.pagination

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.state.runtime.launchCatalogOperation
import f95gm.client.ui.shared.pagination.nextPage
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first

internal fun movePage(runtime: ClientCatalogActionsPort, delta: Int) {
    runtime.launchCatalogOperation {
        val current = runtime.catalog.data.first()
        val nextPage = nextPage(current.page, delta, current.totalPages)
        if (nextPage == current.page) return@launchCatalogOperation
        val next = current.copy(
            page = nextPage,
            editingPage = PageEditMode.BROWSING,
            pageInput = F95Defaults.EMPTY
        )
        runtime.catalog.enqueue { next }
        loadCatalog(runtime, next)
    }
}
