package f95gm.client.actions.filters

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.ApiFallbacks
import f95gm.client.model.filters.ImportedFiltersDocument
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.launchClientOperation
import f95gm.client.state.runtime.ClientFiltersActionsPort
import f95gm.client.state.runtime.launchFilterImportOperation
import f95gm.messages.UiMessages

internal fun importSavedFilters(runtime: ClientFiltersActionsPort) {
    runtime.browser.selectTextFile { payload ->
        runtime.launchFilterImportOperation {
            apiResult { runtime.filtersApi.importFilters(ImportedFiltersDocument.parse(payload)) }
                .onSuccess {
                    runtime.filtersLoading.clearLoaded()
                    loadSavedFiltersIfNeeded(runtime)
                    showToast(runtime, UiMessages.app_quickLinksImported())
                }
                .onFailure { error -> showToast(runtime, error.apiUserMessage(ApiFallbacks.QuickLinksImport)) }
        }
    }
}
