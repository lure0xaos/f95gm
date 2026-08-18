package f95gm.client.actions.marks.pagination

import f95gm.client.state.marks.marks
import f95gm.client.state.runtime.scope
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.launch

internal fun closeMarkedPageEdit() {
    scope.launch {
        marks.enqueue {
            it.copy(
                editingPage = false,
                pageInput = F95Defaults.EMPTY
            )
        }
    }
}
