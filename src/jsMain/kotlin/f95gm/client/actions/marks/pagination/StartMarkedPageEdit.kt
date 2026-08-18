package f95gm.client.actions.marks.pagination

import f95gm.client.model.marks.markedGamesView
import f95gm.client.state.marks.marks
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun startMarkedPageEdit() {
    scope.launch {
        marks.enqueue { current ->
            markedGamesView(current).let { view ->
                current.copy(
                    editingPage = true,
                    pageInput = view.page.toString()
                )
            }
        }
    }
}
