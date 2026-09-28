package f95gm.client.actions.marks.editing

import f95gm.client.state.runtime.ClientMarksActionsPort
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Sort
import f95gm.client.model.runtime.PageInput
import kotlinx.coroutines.launch

internal fun selectMarkedSort(runtime: ClientMarksActionsPort, sort: F95Sort) {
    runtime.launchEffect {
        runtime.marks.enqueue {
            it.copy(
                sort = sort,
                page = F95Defaults.FIRST_PAGE,
                editingPage = PageEditMode.BROWSING,
                pageInput = PageInput.EMPTY
            )
        }
    }
}
