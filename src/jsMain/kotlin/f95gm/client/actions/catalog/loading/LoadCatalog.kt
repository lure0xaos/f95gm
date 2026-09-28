package f95gm.client.actions.catalog.loading

import f95gm.client.actions.catalog.query.catalogPath
import f95gm.client.data.api.apiErrorFallback
import f95gm.client.data.api.ApiFallbacks
import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.model.catalog.CatalogState
import f95gm.client.model.catalog.beginCatalogLoad
import f95gm.client.model.catalog.completeCatalogLoad
import f95gm.client.model.catalog.failCatalogLoad
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.AsyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

internal suspend fun loadCatalog(runtime: ClientCatalogActionsPort, requestState: CatalogState? = null) {
    val catalog = runtime.catalog
    val state = requestState ?: catalog.data.first()
    val requestToken = runtime.requests.beginRequest(ClientJobKey.CATALOG_LOAD)
    catalog.enqueue { it.beginCatalogLoad() }
    try {
        apiResult {
            runtime.catalogApi.catalog(catalogPath(runtime, state))
        }.onSuccess { result ->
            if (!runtime.requests.isCurrentRequest(ClientJobKey.CATALOG_LOAD, requestToken)) return@onSuccess
            catalog.enqueue { it.completeCatalogLoad(result, runtime.seenCatalogThreadIds.markNew(result.items)) }
        }.onFailure { error ->
            if (!runtime.requests.isCurrentRequest(ClientJobKey.CATALOG_LOAD, requestToken)) return@onFailure
            catalog.enqueue { it.failCatalogLoad(error.apiUserMessage(ApiFallbacks.Generic)) }
        }
    } catch (cancelled: CancellationException) {
        if (runtime.requests.isCurrentRequest(ClientJobKey.CATALOG_LOAD, requestToken)) {
            catalog.enqueue { it.copy(async = AsyncState.Idle) }
        }
        throw cancelled
    }
}
