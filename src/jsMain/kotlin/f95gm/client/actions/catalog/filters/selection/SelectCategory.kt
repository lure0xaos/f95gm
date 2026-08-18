package f95gm.client.actions.catalog.filters.selection

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.actions.catalog.loading.loadFilterOptions
import f95gm.client.routing.navigation.latestUpdatesRoute
import f95gm.client.routing.navigation.router
import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun selectCategory(category: String) {
    scope.launch {
        val next = catalog.data.first().copy(category = F95Category.fromWire(category), page = F95Defaults.FIRST_PAGE)
        catalog.enqueue { next.copy(loading = true, error = "") }
        router.navTo(latestUpdatesRoute)
        loadFilterOptions(next.category)
        loadCatalog(next)
    }
}
