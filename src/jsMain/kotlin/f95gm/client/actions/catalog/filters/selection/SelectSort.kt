package f95gm.client.actions.catalog.filters.selection

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.state.runtime.launchCatalogOperation
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Sort
import kotlinx.coroutines.flow.first

internal fun selectSort(runtime: ClientCatalogActionsPort, sort: F95Sort) {
    runtime.launchCatalogOperation {
        val next = runtime.catalog.data.first().copy(sort = sort, page = F95Defaults.FIRST_PAGE)
        runtime.catalog.enqueue { next }
        loadCatalog(runtime, next)
    }
}
