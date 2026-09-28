package f95gm.client.data.filters

import f95gm.client.model.catalog.CatalogState
import f95gm.domain.api.FilterOptionRequest
import f95gm.domain.api.SavedFilterRequest
import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName

internal fun savedFilterBody(name: String, state: CatalogState): SavedFilterRequest =
    SavedFilterRequest(
        name = F95OptionName(name),
        category = state.category,
        sort = state.sort,
        search = state.search.value,
        creatorSearch = state.creatorSearch.value,
        searchMode = state.searchMode,
        selectedTags = state.selectedTags.map { FilterOptionRequest(F95OptionId(it.id.value), it.name) },
        excludedTags = state.excludedTags.map { FilterOptionRequest(F95OptionId(it.id.value), it.name) },
        selectedPrefixes = state.selectedPrefixes.map { FilterOptionRequest(F95OptionId(it.id.value), it.name) },
        excludedPrefixes = state.excludedPrefixes.map { FilterOptionRequest(F95OptionId(it.id.value), it.name) },
        tagType = state.tagType,
        dateLimit = state.dateLimit.value
    )
