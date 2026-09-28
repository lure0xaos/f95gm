package f95gm.client.actions.marks.editing

import f95gm.client.state.runtime.ClientMarksActionsPort
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.launch
import f95gm.client.model.runtime.MarkSearchQuery
import f95gm.client.model.runtime.PageInput

internal fun updateMarkedSearch(runtime: ClientMarksActionsPort, search: String) {
    runtime.launchEffect {
        runtime.marks.enqueue {
            it.copy(
                search = MarkSearchQuery(search),
                page = F95Defaults.FIRST_PAGE,
                editingPage = PageEditMode.BROWSING,
                pageInput = PageInput.EMPTY
            )
        }
    }
}
