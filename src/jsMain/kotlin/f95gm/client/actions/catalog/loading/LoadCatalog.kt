package f95gm.client.actions.catalog.loading

import f95gm.client.actions.catalog.query.catalogPath
import f95gm.client.data.api.apiErrorFallback
import f95gm.client.data.api.getJson
import f95gm.client.data.catalog.parseCatalog
import f95gm.client.model.catalog.CatalogState
import f95gm.client.state.catalog.SeenCatalogThreadIds
import f95gm.client.state.catalog.catalog
import kotlinx.coroutines.flow.first

internal suspend fun loadCatalog(requestState: CatalogState? = null) {
    val state = requestState ?: catalog.data.first()
    catalog.enqueue { it.copy(loading = true, error = "") }
    runCatching {
        val payload = getJson(catalogPath(state))
        parseCatalog(payload)
    }.onSuccess { result ->
        catalog.enqueue {
            it.copy(
                items = SeenCatalogThreadIds.markNew(result.items),
                totalPages = result.totalPages,
                loading = false,
                error = ""
            )
        }
    }.onFailure { error ->
        catalog.enqueue { it.copy(loading = false, error = error.message ?: apiErrorFallback) }
    }
}
