package f95gm.client.actions.filters

import f95gm.client.state.runtime.ClientFiltersActionsPort

internal fun retrySavedFilters(runtime: ClientFiltersActionsPort) {
    runtime.filtersLoading.clearLoaded()
    loadSavedFiltersIfNeeded(runtime)
}
