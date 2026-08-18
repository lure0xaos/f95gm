package f95gm.server.persistence.filters.backup

import f95gm.server.model.filters.SavedFilter
import f95gm.server.model.filters.SavedFilterRequest

internal fun SavedFilter.toSavedFilterRequest(): SavedFilterRequest = SavedFilterRequest(
    name = name,
    category = category,
    sort = sort,
    search = search,
    creatorSearch = creatorSearch,
    searchMode = searchMode,
    selectedTags = selectedTags,
    excludedTags = excludedTags,
    selectedPrefixes = selectedPrefixes,
    excludedPrefixes = excludedPrefixes,
    tagType = tagType,
    dateLimit = dateLimit
)
