package f95gm.client.actions.filters

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.actions.catalog.loading.loadFilterOptions
import f95gm.client.model.filters.SavedFilter
import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun applySavedFilter(filter: SavedFilter) {
    scope.launch {
        val next = catalog.data.first().copy(
            page = F95Defaults.FIRST_PAGE,
            category = filter.category,
            sort = filter.sort,
            search = filter.search,
            creatorSearch = filter.creatorSearch,
            searchMode = filter.searchMode,
            selectedTags = filter.selectedTags,
            excludedTags = filter.excludedTags,
            selectedPrefixes = filter.selectedPrefixes,
            excludedPrefixes = filter.excludedPrefixes,
            tagType = filter.tagType,
            dateLimit = filter.dateLimit,
            tagPicker = "",
            excludedTagPicker = "",
            prefixPickers = emptyMap(),
            excludedPrefixPickers = emptyMap()
        )
        catalog.enqueue { next.copy(loading = true, error = "") }
        loadFilterOptions(filter.category)
        loadCatalog(next)
    }
}
