package f95gm.client.actions.catalog.filters.selection

import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.domain.filters.F95SearchMode
import kotlinx.coroutines.launch

internal fun selectSearchMode(runtime: ClientCatalogActionsPort, mode: F95SearchMode) {
    runtime.launchEffect { runtime.catalog.enqueue { it.copy(searchMode = mode) } }
}
