package f95gm.client.actions.catalog.loading

import f95gm.client.actions.marks.loading.loadMarksIfNeeded
import f95gm.client.state.runtime.ClientCatalogBootstrapActionsPort

internal fun loadCatalogIfNeeded(runtime: ClientCatalogBootstrapActionsPort) {
    if (runtime.catalogLoading.isLoaded()) return
    runtime.catalogLoading.markLoaded()
    launchCatalogLoad(runtime)
    loadMarksIfNeeded(runtime)
}
