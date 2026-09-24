package f95gm.client.actions.catalog.loading

import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.getJson
import f95gm.client.data.catalog.parseCatalogOptions
import f95gm.client.model.catalog.CatalogOptions
import f95gm.client.state.catalog.catalogOptions
import f95gm.domain.api.F95Api
import f95gm.domain.filters.F95Category
import f95gm.messages.UiMessages

internal suspend fun loadFilterOptions(category: F95Category) {
    catalogOptions.enqueue { CatalogOptions(loading = true) }
    apiResult {
        parseCatalogOptions(getJson(F95Api.catalogOptionsPath(category.wireValue)))
    }.onSuccess { options ->
        catalogOptions.enqueue { options }
    }.onFailure { error ->
        catalogOptions.enqueue { CatalogOptions(error = error.apiUserMessage(UiMessages.app_filterOptionsLoadFailed())) }
    }
}
