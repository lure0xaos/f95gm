package f95gm.client.ui.pages.mygames

import dev.fritz2.core.RenderContext
import f95gm.client.actions.marks.pagination.*
import f95gm.client.model.marks.markedGamesView
import f95gm.client.state.marks.marks
import f95gm.client.ui.shared.pagination.PaginationState
import f95gm.client.ui.shared.pagination.paginationControls

internal fun RenderContext.myGamesPagination() {
    paginationControls(
        source = marks.data,
        stateFor = { state ->
            val view = markedGamesView(state)
            PaginationState(view.page, view.totalPages, state.loading, state.editingPage, state.pageInput)
        },
        onFirstPage = { moveMarkedPageTo(1) },
        onPreviousPage = { moveMarkedPage(-1) },
        onPage = { moveMarkedPageTo(it) },
        onNextPage = { moveMarkedPage(1) },
        onLastPage = { moveMarkedPageTo(Int.MAX_VALUE) },
        onStartPageEdit = { startMarkedPageEdit() },
        onPageInput = { updateMarkedPageInput(it) },
        onSubmitPageInput = { submitMarkedPageInput() },
        onClosePageEdit = { closeMarkedPageEdit() }
    )
}
