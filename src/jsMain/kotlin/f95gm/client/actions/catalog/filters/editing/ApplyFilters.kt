package f95gm.client.actions.catalog.filters.editing

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.state.runtime.launchCatalogOperation
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first

internal fun applyFilters(runtime: ClientCatalogActionsPort) {
    runtime.launchCatalogOperation {
        val next = runtime.catalog.data.first().copy(page = F95Defaults.FIRST_PAGE)
        runtime.catalog.enqueue { next }
        loadCatalog(runtime, next)
    }
}
