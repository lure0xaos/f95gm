package f95gm.client.actions.catalog.loading

import f95gm.client.state.runtime.ClientCatalogActionsPort

internal fun retryFilterOptions(runtime: ClientCatalogActionsPort) {
    runtime.catalogLoading.loadedOptionsCategory = null
    loadFilterOptionsIfNeeded(runtime)
}
