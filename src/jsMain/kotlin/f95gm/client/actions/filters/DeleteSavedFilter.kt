package f95gm.client.actions.filters

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.ApiFallbacks
import f95gm.client.routing.navigation.latestUpdatesRoute
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.ClientFiltersActionsPort
import f95gm.client.state.runtime.launchApiOperation
import f95gm.domain.values.F95FilterId
import f95gm.messages.UiMessages

internal fun deleteSavedFilter(runtime: ClientFiltersActionsPort, filterId: F95FilterId) {
    runtime.launchApiOperation(
        key = ClientJobKey.filterDelete(filterId),
        request = { runtime.filtersApi.deleteFilter(filterId) },
        onSuccess = {
                runtime.savedFilters.enqueue { state -> state.copy(items = state.items.filterNot { it.id == filterId }) }
                runtime.uiEffects.navigate(latestUpdatesRoute)
                showToast(runtime, UiMessages.app_quickLinkRemoved())
        },
        onFailure = { error -> showToast(runtime, error.apiUserMessage(ApiFallbacks.FilterRemove)) }
    )
}
