package f95gm.client.actions.filters

import f95gm.client.state.filters.loadedSavedFilters

internal fun retrySavedFilters() {
    loadedSavedFilters = false
    loadSavedFiltersIfNeeded()
}
