package f95gm.client.actions.catalog.loading

import f95gm.client.actions.marks.loading.loadMarksIfNeeded
import f95gm.client.state.session.loaded

internal fun loadCatalogIfNeeded() {
    if (loaded) return
    loaded = true
    launchCatalogLoad()
    loadMarksIfNeeded()
}
