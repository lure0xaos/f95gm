package f95gm.client.actions.catalog.filters.search

import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun updateCreatorSearch(search: String) {
    scope.launch { catalog.enqueue { it.copy(creatorSearch = search) } }
}
