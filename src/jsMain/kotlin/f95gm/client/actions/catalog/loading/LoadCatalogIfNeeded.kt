package f95gm.client.actions.catalog.loading

import f95gm.client.actions.marks.loading.loadMarksIfNeeded
import f95gm.client.state.runtime.scope
import f95gm.client.state.session.loaded
import kotlinx.coroutines.launch

internal fun loadCatalogIfNeeded() {
    if (loaded) return
    loaded = true
    scope.launch { loadCatalog() }
    loadMarksIfNeeded()
}
