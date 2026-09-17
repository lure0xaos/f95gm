package f95gm.client.actions.filters

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.ApiFallbacks
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.launchClientOperation
import f95gm.client.state.runtime.ClientFiltersActionsPort
import f95gm.client.state.runtime.launchFilterExportOperation
import f95gm.messages.UiMessages

internal fun exportSavedFilters(runtime: ClientFiltersActionsPort) {
    runtime.launchFilterExportOperation {
        apiResult { runtime.filtersApi.exportFilters() }
            .onSuccess { payload ->
                runtime.browser.downloadTextFile("f95gm-quick-links.json", payload)
                showToast(runtime, UiMessages.app_quickLinksExported())
            }
            .onFailure { error -> showToast(runtime, error.apiUserMessage(ApiFallbacks.QuickLinksExport)) }
    }
}
