package f95gm.domain.api

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.domain.values.F95EpochMillis
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95OptionName
import kotlinx.serialization.Serializable

@Serializable
data class SavedFilterRequest(
    val id: F95FilterId = F95FilterId(F95Defaults.EMPTY),
    val name: F95OptionName = F95OptionName(F95Defaults.EMPTY),
    val category: F95Category = F95Category.GAMES,
    val sort: F95Sort = F95Sort.DATE,
    val search: String = F95Defaults.EMPTY,
    val creatorSearch: String = F95Defaults.EMPTY,
    val searchMode: F95SearchMode = F95SearchMode.TITLE,
    val selectedTags: List<FilterOptionRequest> = emptyList(),
    val excludedTags: List<FilterOptionRequest> = emptyList(),
    val selectedPrefixes: List<FilterOptionRequest> = emptyList(),
    val excludedPrefixes: List<FilterOptionRequest> = emptyList(),
    val tagType: F95TagType = F95TagType.OR,
    val dateLimit: Int = F95Defaults.ANY_DATE,
    val expectedUpdatedAt: F95EpochMillis? = null
)
