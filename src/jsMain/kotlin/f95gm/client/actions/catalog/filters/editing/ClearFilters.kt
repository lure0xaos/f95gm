package f95gm.client.actions.catalog.filters.editing

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95TagType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun clearFilters() {
    scope.launch {
        val next = catalog.data.first().copy(
            page = F95Defaults.FIRST_PAGE,
            search = F95Defaults.EMPTY,
            creatorSearch = F95Defaults.EMPTY,
            searchMode = F95SearchMode.TITLE,
            selectedTags = emptyList(),
            excludedTags = emptyList(),
            selectedPrefixes = emptyList(),
            excludedPrefixes = emptyList(),
            tagType = F95TagType.OR,
            dateLimit = F95Defaults.ANY_DATE,
            tagPicker = F95Defaults.EMPTY,
            excludedTagPicker = F95Defaults.EMPTY,
            prefixPickers = emptyMap(),
            excludedPrefixPickers = emptyMap()
        )
        catalog.enqueue { next }
        loadCatalog(next)
    }
}
