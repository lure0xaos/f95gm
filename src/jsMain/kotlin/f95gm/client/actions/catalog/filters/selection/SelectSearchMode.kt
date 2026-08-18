package f95gm.client.actions.catalog.filters.selection

import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import f95gm.domain.filters.F95SearchMode
import kotlinx.coroutines.launch

internal fun selectSearchMode(mode: String) {
    scope.launch { catalog.enqueue { it.copy(searchMode = F95SearchMode.fromWire(mode)) } }
}
