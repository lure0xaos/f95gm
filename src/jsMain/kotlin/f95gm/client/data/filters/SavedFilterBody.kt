package f95gm.client.data.filters

import f95gm.client.model.catalog.CatalogState
import f95gm.client.model.filters.SavedFilterOptionBody
import f95gm.client.model.filters.SavedFilterRequestBody
import f95gm.client.state.runtime.clientJson
import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName

internal fun savedFilterBody(name: String, state: CatalogState): String = clientJson.encodeToString(
    SavedFilterRequestBody(
        name = F95OptionName(name),
        category = state.category,
        sort = state.sort,
        search = state.search,
        creatorSearch = state.creatorSearch,
        searchMode = state.searchMode,
        selectedTags = state.selectedTags.map { SavedFilterOptionBody(F95OptionId(it.id.value), it.name) },
        excludedTags = state.excludedTags.map { SavedFilterOptionBody(F95OptionId(it.id.value), it.name) },
        selectedPrefixes = state.selectedPrefixes.map { SavedFilterOptionBody(F95OptionId(it.id.value), it.name) },
        excludedPrefixes = state.excludedPrefixes.map { SavedFilterOptionBody(F95OptionId(it.id.value), it.name) },
        tagType = state.tagType,
        dateLimit = state.dateLimit
    )
)
