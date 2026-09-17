package f95gm.client.actions.marks.pagination

import f95gm.client.model.marks.markedGamesView
import f95gm.client.state.runtime.ClientMarksActionsPort
import f95gm.client.ui.shared.pagination.PageEditMode
import kotlinx.coroutines.launch
import f95gm.client.model.runtime.PageInput

internal fun startMarkedPageEdit(runtime: ClientMarksActionsPort) {
    runtime.launchEffect {
        runtime.marks.enqueue { current ->
            markedGamesView(current).let { view ->
                current.copy(
                    editingPage = PageEditMode.EDITING,
                pageInput = PageInput(view.page.toString())
                )
            }
        }
    }
}
