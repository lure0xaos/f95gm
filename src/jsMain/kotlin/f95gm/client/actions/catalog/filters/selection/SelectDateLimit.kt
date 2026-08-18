package f95gm.client.actions.catalog.filters.selection

import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.scope
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.launch

internal fun selectDateLimit(value: String) {
    scope.launch { catalog.enqueue { it.copy(dateLimit = value.toIntOrNull() ?: F95Defaults.ANY_DATE) } }
}
