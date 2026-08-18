package f95gm.client.model.filters

import f95gm.client.model.catalog.PrefixOption
import f95gm.client.model.catalog.TagOption
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95OptionName

internal data class SavedFilter(
    val id: F95FilterId,
    val name: F95OptionName,
    val category: F95Category,
    val sort: F95Sort,
    val search: String,
    val creatorSearch: String,
    val searchMode: F95SearchMode,
    val selectedTags: List<TagOption>,
    val excludedTags: List<TagOption>,
    val selectedPrefixes: List<PrefixOption>,
    val excludedPrefixes: List<PrefixOption>,
    val tagType: F95TagType,
    val dateLimit: Int,
    val updatedAt: Long
)
