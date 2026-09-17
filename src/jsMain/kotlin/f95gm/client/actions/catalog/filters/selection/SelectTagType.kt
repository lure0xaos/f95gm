package f95gm.client.actions.catalog.filters.selection

import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.domain.filters.F95TagType
import kotlinx.coroutines.launch

internal fun selectTagType(runtime: ClientCatalogActionsPort, value: F95TagType) {
    runtime.launchEffect { runtime.catalog.enqueue { it.copy(tagType = value) } }
}
