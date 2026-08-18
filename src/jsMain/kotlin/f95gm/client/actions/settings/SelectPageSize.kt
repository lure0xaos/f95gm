package f95gm.client.actions.settings

import f95gm.client.actions.catalog.loading.loadCatalog
import f95gm.client.model.settings.PageSize
import f95gm.client.state.catalog.catalog
import f95gm.client.state.marks.marks
import f95gm.client.state.runtime.scope
import f95gm.client.state.settings.appSettings
import f95gm.client.state.settings.persistAppSettings
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun selectPageSize(value: String) {
    val selected = PageSize.fromValue(value.toIntOrNull())
    scope.launch {
        appSettings.enqueue { it.copy(pageSize = selected).also(::persistAppSettings) }
        val current = catalog.data.first()
        val next = current.copy(page = F95Defaults.FIRST_PAGE, totalPages = F95Defaults.FIRST_PAGE)
        catalog.enqueue { next }
        loadCatalog(next)
        marks.enqueue { it.copy(page = F95Defaults.FIRST_PAGE, editingPage = false, pageInput = "") }
    }
}
