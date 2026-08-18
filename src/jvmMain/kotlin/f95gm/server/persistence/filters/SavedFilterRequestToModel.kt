package f95gm.server.persistence.filters

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95OptionName
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.filters.filterCategories
import f95gm.server.config.filters.filterSorts
import f95gm.server.model.filters.SavedFilter
import f95gm.server.model.filters.SavedFilterRequest
import f95gm.server.persistence.filters.alias.filterAlias

internal fun SavedFilterRequest.toSavedFilter(): SavedFilter? {
    val filterName = name.value.trim().take(ServerConstants.FILTER_NAME_LENGTH)
    if (filterName.isBlank()) return null
    return SavedFilter(
        id = F95FilterId(filterAlias(filterName)),
        name = F95OptionName(filterName),
        category = category.takeIf { it.wireValue in filterCategories } ?: F95Category.GAMES,
        sort = sort.takeIf { it.wireValue in filterSorts } ?: F95Sort.DATE,
        search = search.trim().take(ServerConstants.MAX_TEXT_LENGTH),
        creatorSearch = creatorSearch.trim().take(ServerConstants.MAX_TEXT_LENGTH),
        searchMode = searchMode.takeIf { it == F95SearchMode.CREATOR } ?: F95SearchMode.TITLE,
        selectedTags = sanitizeSavedFilterOptions(selectedTags),
        excludedTags = sanitizeSavedFilterOptions(excludedTags),
        selectedPrefixes = sanitizeSavedFilterOptions(selectedPrefixes),
        excludedPrefixes = sanitizeSavedFilterOptions(excludedPrefixes),
        tagType = tagType.takeIf { it == F95TagType.AND } ?: F95TagType.OR,
        dateLimit = dateLimit.coerceIn(F95Defaults.ANY_DATE, F95Defaults.MAX_DATE_DAYS),
        updatedAt = System.currentTimeMillis()
    )
}
