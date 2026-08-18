package f95gm.client.actions.catalog.loading

import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun retryCatalog() {
    scope.launch { loadCatalog() }
}
