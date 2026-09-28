package f95gm.client.actions.marks.pagination

import f95gm.client.state.runtime.ClientMarksActionsPort
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.launch

internal fun closeMarkedPageEdit(runtime: ClientMarksActionsPort) {
    runtime.launchEffect {
        runtime.marks.enqueue {
            it.copy(
                editingPage = PageEditMode.BROWSING,
                pageInput = f95gm.client.model.runtime.PageInput.EMPTY
            )
        }
    }
}
