package f95gm.client.actions.catalog.loading

import f95gm.client.model.catalog.CatalogState
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.clientController
import f95gm.client.state.runtime.launchClientOperation

internal fun launchCatalogLoad(requestState: CatalogState? = null) {
    launchClientOperation(ClientJobKey.CATALOG_LOAD) { loadCatalog(requestState) }
}

internal fun cancelCatalogLoad() {
    clientController.cancelJob(ClientJobKey.CATALOG_LOAD)
}
