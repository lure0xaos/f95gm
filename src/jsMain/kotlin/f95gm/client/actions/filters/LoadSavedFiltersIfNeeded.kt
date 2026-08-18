package f95gm.client.actions.filters

import f95gm.client.data.api.getJson
import f95gm.client.data.filters.parseSavedFilters
import f95gm.client.model.filters.SavedFiltersState
import f95gm.client.state.filters.loadedSavedFilters
import f95gm.client.state.filters.savedFilters
import f95gm.client.state.filters.savedFiltersLoadJob
import f95gm.client.state.runtime.scope
import f95gm.domain.api.F95Api
import f95gm.messages.UiMessages
import kotlinx.coroutines.launch

internal fun loadSavedFiltersIfNeeded() {
    if (loadedSavedFilters) return
    loadedSavedFilters = true
    savedFiltersLoadJob = scope.launch {
        savedFilters.enqueue { SavedFiltersState(loading = true) }
        runCatching { parseSavedFilters(getJson(F95Api.FILTERS)) }
            .onSuccess { items -> savedFilters.enqueue { SavedFiltersState(items = items) } }
            .onFailure { error ->
                savedFilters.enqueue {
                    SavedFiltersState(
                        error = error.message ?: UiMessages.app_savedFiltersLoadFailed()
                    )
                }
            }
    }
}
