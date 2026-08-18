package f95gm.client.actions.catalog.query

import f95gm.client.model.catalog.CatalogState
import f95gm.client.state.settings.appSettings
import f95gm.domain.api.F95Api
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95SearchMode
import kotlinx.coroutines.flow.first

internal suspend fun catalogPath(state: CatalogState): String = buildString {
    append("${F95Api.CATALOG}?${F95Api.CATEGORY}=${queryEncode(state.category.wireValue)}")
    append("&${F95Api.PAGE}=${state.page}&${F95Api.SORT}=${queryEncode(state.sort.wireValue)}")
    append("&${F95Api.ROWS}=${appSettings.data.first().pageSize.value}")
    if (state.searchMode == F95SearchMode.CREATOR) {
        if (state.search.isNotBlank()) append("&${F95Api.CREATOR}=${queryEncode(state.search)}")
    } else if (state.search.isNotBlank()) {
        append("&${F95Api.SEARCH}=${queryEncode(state.search)}")
    }
    if (state.creatorSearch.isNotBlank()) append("&${F95Api.CREATOR}=${queryEncode(state.creatorSearch)}")
    if (state.dateLimit > F95Defaults.ANY_DATE) append("&${F95Api.DATE}=${state.dateLimit}")
    if (state.selectedTags.isNotEmpty() || state.excludedTags.isNotEmpty()) append("&${F95Api.TAG_TYPE}=${state.tagType.wireValue}")
    state.selectedTags.forEach { append("&${F95Api.TAGS}=${queryEncode(it.id.value)}") }
    state.excludedTags.forEach { append("&${F95Api.EXCLUDED_TAGS}=${queryEncode(it.id.value)}") }
    state.selectedPrefixes.forEach { append("&${F95Api.PREFIXES}=${queryEncode(it.id.value)}") }
    state.excludedPrefixes.forEach { append("&${F95Api.EXCLUDED_PREFIXES}=${queryEncode(it.id.value)}") }
}
