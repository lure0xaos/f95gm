package f95gm.client.actions.marks.pagination

import f95gm.client.model.marks.markedGamesView
import f95gm.client.state.marks.marks
import f95gm.client.state.runtime.scope
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun submitMarkedPageInput() {
    scope.launch {
        val current = marks.data.first()
        val view = markedGamesView(current)
        val nextPage = current.pageInput.toIntOrNull()
            ?.coerceIn(F95Defaults.FIRST_PAGE, view.totalPages)
            ?: view.page
        marks.enqueue {
            it.copy(
                page = nextPage,
                editingPage = false,
                pageInput = F95Defaults.EMPTY
            )
        }
    }
}
