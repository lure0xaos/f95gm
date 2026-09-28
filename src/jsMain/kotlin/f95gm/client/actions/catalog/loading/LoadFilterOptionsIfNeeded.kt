package f95gm.client.actions.catalog.loading

import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.state.runtime.launchFilterOptionsOperation
import kotlinx.coroutines.flow.first
import f95gm.client.model.catalog.error

internal fun loadFilterOptionsIfNeeded(runtime: ClientCatalogActionsPort) {
    val category = runtime.catalog.current.category
    if (runtime.catalogLoading.loadedOptionsCategory == category &&
        runtime.requests.isJobActive(ClientJobKey.FILTER_OPTIONS_LOAD)
    ) return

    runtime.launchFilterOptionsOperation {
        val category = runtime.catalog.data.first().category
        if (runtime.catalogLoading.loadedOptionsCategory == category && runtime.catalogOptions.data.first().error.isBlank()) return@launchFilterOptionsOperation
        runtime.catalogLoading.loadedOptionsCategory = category
        loadFilterOptions(runtime, category)
    }
}
