package f95gm.client.actions.marks.pagination

import f95gm.client.state.runtime.ClientMarksActionsPort
import kotlinx.coroutines.launch
import f95gm.client.model.runtime.PageInput

internal fun updateMarkedPageInput(runtime: ClientMarksActionsPort, input: String) {
    runtime.launchEffect {
        runtime.marks.enqueue { it.copy(pageInput = PageInput(input)) }
    }
}
