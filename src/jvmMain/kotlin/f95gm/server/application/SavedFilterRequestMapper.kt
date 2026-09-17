package f95gm.server.application

import f95gm.server.kernel.ServerClock

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95OptionName
import f95gm.server.config.filters.filterCategories
import f95gm.server.config.filters.filterSorts
import f95gm.server.model.EpochMillis
import f95gm.server.model.filters.SavedFilter
import f95gm.domain.api.SavedFilterRequest
import f95gm.server.persistence.filters.alias.filterAlias
import f95gm.server.persistence.filters.sanitizeSavedFilterOptions
import f95gm.server.model.ParseResult
import f95gm.server.config.CatalogLimits
import f95gm.server.config.constants.FilterLimits

internal fun SavedFilterRequest.toSavedFilter(clock: ServerClock): SavedFilter? {
    return when (val result = toSavedFilterResult(clock)) {
        is ParseResult.Success -> result.value
        is ParseResult.Invalid -> null
    }
}

internal fun SavedFilterRequest.toSavedFilterResult(clock: ServerClock): ParseResult<SavedFilter> {
    val filterName = name.value.trim().take(FilterLimits.FILTER_NAME_LENGTH)
    if (filterName.isBlank()) return ParseResult.Invalid("Filter name must not be blank")
    return ParseResult.Success(SavedFilter(
        id = F95FilterId(filterAlias(filterName)),
        name = F95OptionName(filterName),
        category = category.takeIf { it.wireValue in filterCategories } ?: F95Category.GAMES,
        sort = sort.takeIf { it.wireValue in filterSorts } ?: F95Sort.DATE,
        search = search.trim().take(CatalogLimits.MAX_TEXT_LENGTH),
        creatorSearch = creatorSearch.trim().take(CatalogLimits.MAX_TEXT_LENGTH),
        searchMode = searchMode.takeIf { it == F95SearchMode.CREATOR } ?: F95SearchMode.TITLE,
        selectedTags = sanitizeSavedFilterOptions(selectedTags),
        excludedTags = sanitizeSavedFilterOptions(excludedTags),
        selectedPrefixes = sanitizeSavedFilterOptions(selectedPrefixes),
        excludedPrefixes = sanitizeSavedFilterOptions(excludedPrefixes),
        tagType = tagType.takeIf { it == F95TagType.AND } ?: F95TagType.OR,
        dateLimit = dateLimit.coerceIn(F95Defaults.ANY_DATE, F95Defaults.MAX_DATE_DAYS),
        updatedAt = clock.epochMillis()
    ))
}

