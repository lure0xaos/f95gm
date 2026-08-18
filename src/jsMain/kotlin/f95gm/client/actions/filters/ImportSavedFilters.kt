package f95gm.client.actions.filters

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.postJson
import f95gm.client.data.transfer.selectTextFile
import f95gm.client.state.filters.loadedSavedFilters
import f95gm.client.state.runtime.scope
import f95gm.domain.api.F95Api
import f95gm.messages.UiMessages
import kotlinx.coroutines.launch

internal fun importSavedFilters() {
    selectTextFile { payload ->
        scope.launch {
            runCatching { postJson(F95Api.FILTERS_IMPORT, payload) }
                .onSuccess {
                    loadedSavedFilters = false
                    loadSavedFiltersIfNeeded()
                    showToast(UiMessages.app_quickLinksImported())
                }
                .onFailure { error -> showToast(error.message ?: UiMessages.app_quickLinksImportFailed()) }
        }
    }
}
