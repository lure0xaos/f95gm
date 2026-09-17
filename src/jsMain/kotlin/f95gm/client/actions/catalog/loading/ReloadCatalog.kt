package f95gm.client.actions.catalog.loading

import f95gm.client.state.runtime.ClientCatalogActionsPort

internal fun reloadCatalog(runtime: ClientCatalogActionsPort) {
    launchCatalogLoad(runtime)
}
