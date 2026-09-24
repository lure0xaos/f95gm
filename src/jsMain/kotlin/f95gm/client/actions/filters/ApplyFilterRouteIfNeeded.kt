package f95gm.client.actions.filters

import f95gm.client.actions.feedback.showToast
import f95gm.client.routing.navigation.latestUpdatesRoute
import f95gm.client.routing.navigation.router
import f95gm.client.state.filters.appliedFilterId
import f95gm.client.state.filters.savedFilters
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.clientOperation
import f95gm.client.state.runtime.scope
import f95gm.domain.values.F95FilterId
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun applyFilterRouteIfNeeded(filterId: String?) {
    val typedFilterId = filterId?.takeIf { it.isNotBlank() }?.let(::F95FilterId)
    if (typedFilterId == null) {
        appliedFilterId = null
        return
    }
    if (appliedFilterId == typedFilterId) return
    appliedFilterId = typedFilterId
    scope.launch {
        clientOperation(ClientJobKey.SAVED_FILTERS_LOAD)?.join()
        val filter = savedFilters.data.first().items.firstOrNull { it.id == typedFilterId }
        if (filter == null) {
            showToast(UiMessages.app_savedFilterUnavailable())
            router.navTo(latestUpdatesRoute)
        } else {
            applySavedFilter(filter)
        }
    }
}
