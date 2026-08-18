package f95gm.client.actions.filters

import f95gm.client.actions.feedback.showToast
import f95gm.client.routing.navigation.latestUpdatesRoute
import f95gm.client.routing.navigation.router
import f95gm.client.state.filters.appliedFilterId
import f95gm.client.state.filters.savedFilters
import f95gm.client.state.filters.savedFiltersLoadJob
import f95gm.client.state.runtime.scope
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun applyFilterRouteIfNeeded(filterId: String?) {
    if (filterId.isNullOrBlank()) {
        appliedFilterId = null
        return
    }
    if (appliedFilterId == filterId) return
    appliedFilterId = filterId
    scope.launch {
        savedFiltersLoadJob?.join()
        val filter = savedFilters.data.first().items.firstOrNull { it.id.value == filterId }
        if (filter == null) {
            showToast(UiMessages.app_savedFilterUnavailable())
            router.navTo(latestUpdatesRoute)
        } else {
            applySavedFilter(filter)
        }
    }
}
