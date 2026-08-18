package f95gm.client.actions.marks.pagination

import f95gm.client.state.marks.marks
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun updateMarkedPageInput(input: String) {
    scope.launch {
        marks.enqueue { it.copy(pageInput = input) }
    }
}
