package f95gm.client.ui.pages.mygames

import dev.fritz2.core.RenderContext
import f95gm.client.model.marks.markedGamesView
import f95gm.client.ui.shared.pagination.PaginationState
import f95gm.client.ui.shared.pagination.paginationControls
import f95gm.client.state.runtime.ClientMarksUiPort
import f95gm.client.model.marks.loading

internal fun RenderContext.myGamesPagination(runtime: ClientMarksUiPort) {
    val marks = runtime.marksView
    paginationControls(
        source = marks.data,
        stateFor = { state ->
            val view = markedGamesView(state)
            PaginationState(view.page, view.totalPages, state.loading, state.editingPage, state.pageInput.value)
        },
        onFirstPage = { runtime.marksCommands.moveMarkedPageTo(1) },
        onPreviousPage = { runtime.marksCommands.moveMarkedPage(-1) },
        onPage = { runtime.marksCommands.moveMarkedPageTo(it) },
        onNextPage = { runtime.marksCommands.moveMarkedPage(1) },
        onLastPage = { runtime.marksCommands.moveMarkedPageTo(Int.MAX_VALUE) },
        onStartPageEdit = { runtime.marksCommands.startMarkedPageEdit() },
        onPageInput = { runtime.marksCommands.updateMarkedPageInput(it) },
        onSubmitPageInput = { runtime.marksCommands.submitMarkedPageInput() },
        onClosePageEdit = { runtime.marksCommands.closeMarkedPageEdit() }
    )
}
