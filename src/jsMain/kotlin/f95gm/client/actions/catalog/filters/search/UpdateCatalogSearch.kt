package f95gm.client.actions.catalog.filters.search

import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.model.runtime.CatalogSearchQuery
import kotlinx.coroutines.launch

internal fun updateCatalogSearch(runtime: ClientCatalogActionsPort, search: String) {
    runtime.launchEffect { runtime.catalog.enqueue { it.copy(search = CatalogSearchQuery(search)) } }
}
