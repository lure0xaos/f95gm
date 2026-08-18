package f95gm.client.actions.catalog.pagination

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun movePageTo(page: Int) {
    scope.launch {
        val current = catalog.data.first()
        val nextPage = page.coerceIn(F95Defaults.FIRST_PAGE, current.totalPages)
        if (nextPage == current.page && !current.editingPage) return@launch
        val next = current.copy(
            page = nextPage,
            editingPage = false,
            pageInput = F95Defaults.EMPTY
        )
        catalog.enqueue { next }
        loadCatalog(next)
    }
}
