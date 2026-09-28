package f95gm.client.actions.catalog.loading

import f95gm.client.model.catalog.CatalogState
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.launchCatalogOperation

internal fun launchCatalogLoad(runtime: ClientCatalogActionsPort, requestState: CatalogState? = null, beforeLoad: suspend () -> Unit = {}) {
    runtime.launchCatalogOperation {
        beforeLoad()
        loadCatalog(runtime, requestState)
    }
}

internal fun cancelCatalogLoad(runtime: f95gm.client.state.runtime.ClientOperationPort) {
    runtime.requests.cancelJob(ClientJobKey.CATALOG_LOAD)
}
