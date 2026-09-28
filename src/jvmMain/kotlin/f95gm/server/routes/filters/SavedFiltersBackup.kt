package f95gm.server.routes.filters

import f95gm.server.model.filters.SavedFilter
import f95gm.server.model.filters.QuickLinksBackup
import f95gm.domain.api.SavedFilterRequest
import f95gm.server.http.serialization.upstreamJson

private fun SavedFilter.toSavedFilterRequest(): SavedFilterRequest = SavedFilterRequest(
    name = name, category = category, sort = sort, search = search, creatorSearch = creatorSearch,
    searchMode = searchMode, selectedTags = selectedTags, excludedTags = excludedTags,
    selectedPrefixes = selectedPrefixes, excludedPrefixes = excludedPrefixes, tagType = tagType, dateLimit = dateLimit
)

internal fun savedFiltersBackupJson(filters: List<SavedFilter>): String = upstreamJson.encodeToString(
    QuickLinksBackup(quickLinks = filters.map { it.toSavedFilterRequest() })
)

