package f95gm.client.model.catalog

import f95gm.client.model.runtime.CatalogSearchQuery
import f95gm.client.model.runtime.DateLimit

internal object CatalogSelectors {
    fun search(state: CatalogState): String = state.search.value
    fun creatorSearch(state: CatalogState): String = state.creatorSearch.value
    fun dateLimit(state: CatalogState): Int = state.dateLimit.value
    fun hasActiveTextSearch(state: CatalogState): Boolean =
        state.search != CatalogSearchQuery.EMPTY || state.creatorSearch != CatalogSearchQuery.EMPTY
    fun hasDateLimit(state: CatalogState): Boolean = state.dateLimit != DateLimit.ANY
}
