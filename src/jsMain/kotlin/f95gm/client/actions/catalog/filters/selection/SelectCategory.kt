package f95gm.client.actions.catalog.filters.selection

import f95gm.client.actions.catalog.loading.loadFilterOptions
import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.launchCatalogOperation
import f95gm.client.routing.navigation.latestUpdatesRoute
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Category
import kotlinx.coroutines.flow.first
import f95gm.client.state.runtime.AsyncState

internal fun selectCategory(runtime: ClientCatalogActionsPort, category: F95Category) {
    runtime.launchCatalogOperation {
        val next = runtime.catalog.data.first().copy(category = category, page = F95Defaults.FIRST_PAGE)
        runtime.catalog.enqueue { next.copy(async = AsyncState.Loading) }
        runtime.uiEffects.navigate(latestUpdatesRoute)
        runtime.requests.cancelJob(ClientJobKey.FILTER_OPTIONS_LOAD)
        loadFilterOptions(runtime, next.category)
        loadCatalog(runtime, next)
    }
}
