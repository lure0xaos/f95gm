package f95gm.client.actions.catalog.filters.selection

import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.client.model.runtime.DateLimit
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.launch

internal fun selectDateLimit(runtime: ClientCatalogActionsPort, value: Int) {
    runtime.launchEffect { runtime.catalog.enqueue { it.copy(dateLimit = DateLimit(value)) } }
}
