package f95gm.client.model.filters

import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.domain.values.F95OptionName
import kotlinx.serialization.Serializable

@Serializable
internal data class SavedFilterRequestBody(
    val name: F95OptionName,
    val category: F95Category,
    val sort: F95Sort,
    val search: String,
    val creatorSearch: String,
    val searchMode: F95SearchMode,
    val selectedTags: List<SavedFilterOptionBody>,
    val excludedTags: List<SavedFilterOptionBody>,
    val selectedPrefixes: List<SavedFilterOptionBody>,
    val excludedPrefixes: List<SavedFilterOptionBody>,
    val tagType: F95TagType,
    val dateLimit: Int
)
