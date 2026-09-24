package f95gm.client.actions.filters

import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.getJson
import f95gm.client.data.filters.parseSavedFilters
import f95gm.client.model.filters.SavedFiltersState
import f95gm.client.state.filters.loadedSavedFilters
import f95gm.client.state.filters.savedFilters
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.launchClientOperation
import f95gm.domain.api.F95Api
import f95gm.messages.UiMessages

internal fun loadSavedFiltersIfNeeded() {
    if (loadedSavedFilters) return
    loadedSavedFilters = true
    launchClientOperation(ClientJobKey.SAVED_FILTERS_LOAD) {
        savedFilters.enqueue { SavedFiltersState(loading = true) }
        apiResult { parseSavedFilters(getJson(F95Api.FILTERS)) }
            .onSuccess { items -> savedFilters.enqueue { SavedFiltersState(items = items) } }
            .onFailure { error ->
                savedFilters.enqueue {
                    SavedFiltersState(
                        error = error.apiUserMessage(UiMessages.app_savedFiltersLoadFailed())
                    )
                }
            }
    }
}
