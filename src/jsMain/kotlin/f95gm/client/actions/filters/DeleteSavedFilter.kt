package f95gm.client.actions.filters

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.deleteJson
import f95gm.client.routing.navigation.latestUpdatesRoute
import f95gm.client.routing.navigation.router
import f95gm.client.state.filters.savedFilters
import f95gm.client.state.runtime.scope
import f95gm.domain.api.F95Api
import f95gm.domain.values.F95FilterId
import f95gm.messages.UiMessages
import kotlinx.coroutines.launch

internal fun deleteSavedFilter(filterId: F95FilterId) {
    scope.launch {
        apiResult { deleteJson(F95Api.filterPath(filterId)) }
            .onSuccess {
                savedFilters.enqueue { state -> state.copy(items = state.items.filterNot { it.id == filterId }) }
                router.navTo(latestUpdatesRoute)
                showToast(UiMessages.app_quickLinkRemoved())
            }
            .onFailure { error -> showToast(error.apiUserMessage(UiMessages.app_filterRemoveFailed())) }
    }
}
