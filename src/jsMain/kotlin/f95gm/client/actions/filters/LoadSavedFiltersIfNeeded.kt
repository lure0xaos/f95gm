package f95gm.client.actions.filters

import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.ApiFallbacks
import f95gm.client.model.filters.SavedFiltersState
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.ClientFiltersActionsPort
import f95gm.client.state.runtime.launchSavedFiltersLoadOperation
import f95gm.messages.UiMessages
import f95gm.client.state.runtime.AsyncState

internal fun loadSavedFiltersIfNeeded(runtime: ClientFiltersActionsPort) {
    if (runtime.filtersLoading.isLoaded()) return
    runtime.filtersLoading.markLoaded()
    runtime.launchSavedFiltersLoadOperation {
        val savedFilters = runtime.savedFilters
        savedFilters.enqueue { SavedFiltersState(async = AsyncState.Loading) }
        apiResult { runtime.filtersApi.filters() }
            .onSuccess { items -> savedFilters.enqueue { SavedFiltersState(items = items) } }
            .onFailure { error ->
                savedFilters.enqueue {
                    SavedFiltersState(
                        async = AsyncState.Failure(error.apiUserMessage(ApiFallbacks.SavedFilters))
                    )
                }
            }
    }
}
