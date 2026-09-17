package f95gm.client.actions.marks.pagination

import f95gm.client.model.marks.markedGamesView
import f95gm.client.state.runtime.ClientMarksActionsPort
import f95gm.client.ui.shared.pagination.boundedPage
import f95gm.client.ui.shared.pagination.PageNumber
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import f95gm.client.model.runtime.PageInput

internal fun moveMarkedPageTo(runtime: ClientMarksActionsPort, page: Int) = moveMarkedPageTo(runtime, PageNumber(page))

internal fun moveMarkedPageTo(runtime: ClientMarksActionsPort, page: PageNumber) {
    runtime.launchEffect {
        val current = runtime.marks.data.first()
        val view = markedGamesView(current)
        val nextPage = boundedPage(page.value, view.totalPages)
        if (nextPage == current.page && current.editingPage == PageEditMode.BROWSING) return@launchEffect
        runtime.marks.enqueue {
            it.copy(
                page = nextPage,
                editingPage = PageEditMode.BROWSING,
                pageInput = PageInput.EMPTY
            )
        }
    }
}
