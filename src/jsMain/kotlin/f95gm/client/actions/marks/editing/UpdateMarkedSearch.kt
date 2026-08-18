package f95gm.client.actions.marks.editing

import f95gm.client.state.marks.marks
import f95gm.client.state.runtime.scope
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.launch

internal fun updateMarkedSearch(search: String) {
    scope.launch {
        marks.enqueue {
            it.copy(
                search = search,
                page = F95Defaults.FIRST_PAGE,
                editingPage = false,
                pageInput = F95Defaults.EMPTY
            )
        }
    }
}
