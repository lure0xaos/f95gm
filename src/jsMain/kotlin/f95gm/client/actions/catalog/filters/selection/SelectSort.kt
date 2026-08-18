package f95gm.client.actions.catalog.filters.selection

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Sort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun selectSort(sort: String) {
    scope.launch {
        val next = catalog.data.first().copy(sort = F95Sort.fromWire(sort), page = F95Defaults.FIRST_PAGE)
        catalog.enqueue { next }
        loadCatalog(next)
    }
}
