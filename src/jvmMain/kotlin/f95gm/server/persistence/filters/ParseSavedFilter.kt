package f95gm.server.persistence.filters

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.server.config.filters.filterCategories
import f95gm.server.config.filters.filterSorts
import f95gm.server.model.filters.SavedFilter

internal fun parseSavedFilter(value: SavedFilter): SavedFilter? {
    if (value.name.value.isBlank()) return null
    return value.copy(
        category = value.category.takeIf { it.wireValue in filterCategories } ?: F95Category.GAMES,
        sort = value.sort.takeIf { it.wireValue in filterSorts } ?: F95Sort.DATE,
        searchMode = value.searchMode.takeIf { it == F95SearchMode.CREATOR } ?: F95SearchMode.TITLE,
        selectedTags = sanitizeSavedFilterOptions(value.selectedTags),
        excludedTags = sanitizeSavedFilterOptions(value.excludedTags),
        selectedPrefixes = sanitizeSavedFilterOptions(value.selectedPrefixes),
        excludedPrefixes = sanitizeSavedFilterOptions(value.excludedPrefixes),
        tagType = value.tagType.takeIf { it == F95TagType.AND } ?: F95TagType.OR,
        dateLimit = value.dateLimit.coerceIn(F95Defaults.ANY_DATE, F95Defaults.MAX_DATE_DAYS)
    )
}
