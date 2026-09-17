package f95gm.client.model.catalog

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.domain.values.F95OptionId
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.client.state.runtime.AsyncState
import f95gm.client.state.runtime.isLoading
import f95gm.client.state.runtime.errorMessage
import f95gm.client.model.runtime.CatalogSearchQuery
import f95gm.client.model.runtime.DateLimit

internal data class CatalogState(
    val items: List<Game> = emptyList(),
    val page: Int = F95Defaults.FIRST_PAGE,
    val editingPage: PageEditMode = PageEditMode.BROWSING,
    val pageInput: String = F95Defaults.EMPTY,
    val totalPages: Int = F95Defaults.FIRST_PAGE,
    val category: F95Category = F95Category.GAMES,
    val sort: F95Sort = F95Sort.DATE,
    val search: CatalogSearchQuery = CatalogSearchQuery.EMPTY,
    val creatorSearch: CatalogSearchQuery = CatalogSearchQuery.EMPTY,
    val searchMode: F95SearchMode = F95SearchMode.TITLE,
    val selectedTags: List<TagOption> = emptyList(),
    val excludedTags: List<TagOption> = emptyList(),
    val selectedPrefixes: List<PrefixOption> = emptyList(),
    val excludedPrefixes: List<PrefixOption> = emptyList(),
    val tagType: F95TagType = F95TagType.OR,
    val dateLimit: DateLimit = DateLimit.ANY,
    val tagPicker: String = F95Defaults.EMPTY,
    val excludedTagPicker: String = F95Defaults.EMPTY,
    val prefixPickers: Map<F95OptionId, String> = emptyMap(),
    val excludedPrefixPickers: Map<F95OptionId, String> = emptyMap(),
    val async: AsyncState<Unit> = AsyncState.Idle
)

internal val CatalogState.loading: Boolean get() = async.isLoading
internal val CatalogState.error: String get() = async.errorMessage
