package f95gm.client.actions.catalog.loading

import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.ApiFallbacks
import f95gm.client.model.catalog.CatalogOptions
import f95gm.domain.filters.F95Category
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.messages.UiMessages
import f95gm.client.state.runtime.AsyncState

internal suspend fun loadFilterOptions(runtime: ClientCatalogActionsPort, category: F95Category) {
    val catalogOptions = runtime.catalogOptions
    catalogOptions.enqueue { CatalogOptions(async = AsyncState.Loading) }
    apiResult {
        runtime.catalogApi.catalogOptions(category)
    }.onSuccess { options ->
        catalogOptions.enqueue { options }
    }.onFailure { error ->
        catalogOptions.enqueue { CatalogOptions(async = AsyncState.Failure(error.apiUserMessage(ApiFallbacks.FilterOptions))) }
    }
}
