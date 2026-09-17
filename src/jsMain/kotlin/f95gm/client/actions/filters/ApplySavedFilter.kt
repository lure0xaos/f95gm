package f95gm.client.actions.filters

import f95gm.client.actions.catalog.loading.loadFilterOptions
import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.model.filters.SavedFilter
import f95gm.client.state.runtime.ClientFiltersActionsPort
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.launchCatalogOperation
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first
import f95gm.client.state.runtime.AsyncState
import f95gm.client.model.runtime.CatalogSearchQuery
import f95gm.client.model.runtime.DateLimit

internal fun applySavedFilter(runtime: ClientFiltersActionsPort, filter: SavedFilter) {
    runtime.launchCatalogOperation {
        val next = runtime.catalog.data.first().copy(
            page = F95Defaults.FIRST_PAGE,
            category = filter.category,
            sort = filter.sort,
            search = CatalogSearchQuery(filter.search),
            creatorSearch = CatalogSearchQuery(filter.creatorSearch),
            searchMode = filter.searchMode,
            selectedTags = filter.selectedTags,
            excludedTags = filter.excludedTags,
            selectedPrefixes = filter.selectedPrefixes,
            excludedPrefixes = filter.excludedPrefixes,
            tagType = filter.tagType,
            dateLimit = DateLimit(filter.dateLimit),
            tagPicker = "",
            excludedTagPicker = "",
            prefixPickers = emptyMap(),
            excludedPrefixPickers = emptyMap()
        )
        runtime.catalog.enqueue { next.copy(async = AsyncState.Loading) }
        runtime.requests.cancelJob(ClientJobKey.FILTER_OPTIONS_LOAD)
        loadFilterOptions(runtime, filter.category)
        loadCatalog(runtime, next)
    }
}
