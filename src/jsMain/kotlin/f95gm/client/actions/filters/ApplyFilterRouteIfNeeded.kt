package f95gm.client.actions.filters

import f95gm.client.actions.feedback.showToast
import f95gm.client.routing.navigation.latestUpdatesRoute
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.ClientFiltersActionsPort
import f95gm.domain.values.F95FilterId
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun applyFilterRouteIfNeeded(runtime: ClientFiltersActionsPort, filterId: String?) {
    val typedFilterId = filterId?.takeIf { it.isNotBlank() }?.let(::F95FilterId)
    if (typedFilterId == null) {
        runtime.filtersLoading.appliedFilterId = null
        return
    }
    if (runtime.filtersLoading.appliedFilterId == typedFilterId) return
    runtime.filtersLoading.appliedFilterId = typedFilterId
    runtime.launchEffect {
        runtime.requests.currentJob(ClientJobKey.SAVED_FILTERS_LOAD)?.join()
        val filter = runtime.savedFilters.data.first().items.firstOrNull { it.id == typedFilterId }
        if (filter == null) {
            showToast(runtime, UiMessages.app_savedFilterUnavailable())
            runtime.uiEffects.navigate(latestUpdatesRoute)
        } else {
            applySavedFilter(runtime, filter)
        }
    }
}
