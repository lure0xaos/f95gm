package f95gm.client.actions.catalog.filters.editing

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun applyFilters() {
    scope.launch {
        val next = catalog.data.first().copy(page = F95Defaults.FIRST_PAGE)
        catalog.enqueue { next }
        loadCatalog(next)
    }
}
