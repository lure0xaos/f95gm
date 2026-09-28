package f95gm.client.actions.catalog.pagination

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.state.runtime.launchCatalogOperation
import f95gm.client.ui.shared.pagination.PageNumber
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.client.ui.shared.pagination.boundedPage
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first

internal fun movePageTo(runtime: ClientCatalogActionsPort, page: Int) = movePageTo(runtime, PageNumber(page))

internal fun movePageTo(runtime: ClientCatalogActionsPort, page: PageNumber) {
    runtime.launchCatalogOperation {
        val current = runtime.catalog.data.first()
        val nextPage = boundedPage(page.value, current.totalPages)
        if (nextPage == current.page && current.editingPage == PageEditMode.BROWSING) return@launchCatalogOperation
        val next = current.copy(
            page = nextPage,
            editingPage = PageEditMode.BROWSING,
            pageInput = F95Defaults.EMPTY
        )
        runtime.catalog.enqueue { next }
        loadCatalog(runtime, next)
    }
}
