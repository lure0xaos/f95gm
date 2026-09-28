package f95gm.client.actions.marks.pagination

import f95gm.client.model.marks.markedGamesView
import f95gm.client.state.runtime.ClientMarksActionsPort
import f95gm.client.ui.shared.pagination.nextPage
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun moveMarkedPage(runtime: ClientMarksActionsPort, delta: Int) {
    runtime.launchEffect {
        val current = runtime.marks.data.first()
        val view = markedGamesView(current)
        val nextPage = nextPage(view.page, delta, view.totalPages)
        if (nextPage == current.page) return@launchEffect
        runtime.marks.enqueue {
            it.copy(
                page = nextPage,
                editingPage = PageEditMode.BROWSING,
                pageInput = f95gm.client.model.runtime.PageInput.EMPTY
            )
        }
    }
}
