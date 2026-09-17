package f95gm.client.actions.marks.pagination

import f95gm.client.model.marks.markedGamesView
import f95gm.client.state.runtime.ClientMarksActionsPort
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.domain.api.parseIntOrNull
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import f95gm.client.model.runtime.PageInput

internal fun submitMarkedPageInput(runtime: ClientMarksActionsPort) {
    runtime.launchEffect {
        val current = runtime.marks.data.first()
        val view = markedGamesView(current)
        val nextPage = current.pageInput.value.parseIntOrNull()
            ?.coerceIn(F95Defaults.FIRST_PAGE, view.totalPages)
            ?: view.page
        runtime.marks.enqueue {
            it.copy(
                page = nextPage,
                editingPage = PageEditMode.BROWSING,
                pageInput = PageInput.EMPTY
            )
        }
    }
}
