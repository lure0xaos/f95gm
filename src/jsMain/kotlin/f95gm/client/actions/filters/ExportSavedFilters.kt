package f95gm.client.actions.filters

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.getJson
import f95gm.client.data.transfer.downloadTextFile
import f95gm.client.state.runtime.scope
import f95gm.domain.api.F95Api
import f95gm.messages.UiMessages
import kotlinx.coroutines.launch

internal fun exportSavedFilters() {
    scope.launch {
        apiResult { getJson(F95Api.FILTERS_EXPORT) }
            .onSuccess { payload ->
                downloadTextFile("f95gm-quick-links.json", payload)
                showToast(UiMessages.app_quickLinksExported())
            }
            .onFailure { error -> showToast(error.apiUserMessage(UiMessages.app_quickLinksExportFailed())) }
    }
}
