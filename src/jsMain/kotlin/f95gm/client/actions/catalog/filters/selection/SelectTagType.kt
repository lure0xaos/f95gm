package f95gm.client.actions.catalog.filters.selection

import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import f95gm.domain.filters.F95TagType
import kotlinx.coroutines.launch

internal fun selectTagType(value: String) {
    scope.launch { catalog.enqueue { it.copy(tagType = F95TagType.fromWire(value)) } }
}
