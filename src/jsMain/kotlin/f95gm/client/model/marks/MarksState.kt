package f95gm.client.model.marks

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Sort
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.client.state.runtime.AsyncState
import f95gm.client.state.runtime.isLoading
import f95gm.client.state.runtime.errorMessage
import f95gm.client.model.runtime.MarkSearchQuery
import f95gm.client.model.runtime.PageInput

internal data class MarksState(
    val items: List<MarkedGame> = emptyList(),
    val page: Int = F95Defaults.FIRST_PAGE,
    val editingPage: PageEditMode = PageEditMode.BROWSING,
    val pageInput: PageInput = PageInput.EMPTY,
    val sort: F95Sort = F95Sort.DATE,
    val search: MarkSearchQuery = MarkSearchQuery.EMPTY,
    val async: AsyncState<Unit> = AsyncState.Idle
)

internal val MarksState.loading: Boolean get() = async.isLoading
internal val MarksState.checking: Boolean get() = async.isLoading
internal val MarksState.error: String get() = async.errorMessage
