package f95gm.client.actions.catalog.filters.editing

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.model.catalog.resetCatalogSearch
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.state.runtime.launchCatalogOperation
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95TagType
import kotlinx.coroutines.flow.first

internal fun clearFilters(runtime: ClientCatalogActionsPort) {
    runtime.launchCatalogOperation {
        val next = runtime.catalog.data.first().resetCatalogSearch().copy(
            page = F95Defaults.FIRST_PAGE,
            searchMode = F95SearchMode.TITLE,
            selectedTags = emptyList(),
            excludedTags = emptyList(),
            selectedPrefixes = emptyList(),
            excludedPrefixes = emptyList(),
            tagType = F95TagType.OR,
            tagPicker = F95Defaults.EMPTY,
            excludedTagPicker = F95Defaults.EMPTY,
            prefixPickers = emptyMap(),
            excludedPrefixPickers = emptyMap()
        )
        runtime.catalog.enqueue { next }
        loadCatalog(runtime, next)
    }
}
