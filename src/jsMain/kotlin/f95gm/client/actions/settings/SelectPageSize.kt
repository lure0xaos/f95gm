package f95gm.client.actions.settings

import f95gm.client.actions.catalog.loading.launchCatalogLoad
import f95gm.client.model.settings.PageSize
import f95gm.client.state.runtime.ClientSettingsActionsPort
import f95gm.client.ui.shared.pagination.PageEditMode
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun selectPageSize(runtime: ClientSettingsActionsPort, selected: PageSize) {
    runtime.launchEffect {
        runtime.settingsStore.update { it.copy(pageSize = selected) }
        val current = runtime.catalog.data.first()
        val next = current.copy(page = F95Defaults.FIRST_PAGE, totalPages = F95Defaults.FIRST_PAGE)
        runtime.catalog.enqueue { next }
        launchCatalogLoad(runtime, next)
        runtime.marks.enqueue { it.copy(page = F95Defaults.FIRST_PAGE, editingPage = PageEditMode.BROWSING, pageInput = f95gm.client.model.runtime.PageInput.EMPTY) }
    }
}
