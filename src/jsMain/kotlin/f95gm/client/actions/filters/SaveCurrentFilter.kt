package f95gm.client.actions.filters

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.ApiFallbacks
import f95gm.client.data.filters.savedFilterBody
import f95gm.client.model.filters.SavedFiltersState
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.launchClientOperation
import f95gm.client.state.runtime.ClientFiltersActionsPort
import f95gm.client.state.runtime.launchFilterSaveOperation
import f95gm.client.validation.ClientInputValidation
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

internal fun saveCurrentFilter(runtime: ClientFiltersActionsPort) {
    runtime.launchFilterSaveOperation {
        val (rawName, catalogState) = combine(
            runtime.savedFilterName.data,
            runtime.catalog.data
        ) { name, state -> name to state }.first()
        val name = ClientInputValidation.filterName(rawName)
        if (name == null) {
            showToast(runtime, UiMessages.app_savedFilterNameRequired())
            return@launchFilterSaveOperation
        }
        apiResult {
            runtime.filtersApi.saveFilter(savedFilterBody(name, catalogState))
        }.onSuccess { payload ->
            val saved = payload.firstOrNull()
            if (saved == null) {
                showToast(runtime, UiMessages.app_savedFilterResponseInvalid())
                return@onSuccess
            }
            runtime.savedFilters.enqueue { state ->
                SavedFiltersState(
                    items = listOf(saved) + state.items.filterNot {
                        it.name.value.equals(
                            saved.name.value,
                            ignoreCase = true
                        )
                    }
                )
            }
            runtime.savedFilterName.enqueue { "" }
            showToast(runtime, UiMessages.app_savedQuickLink(saved.name.value))
        }.onFailure { error ->
            showToast(runtime, error.apiUserMessage(ApiFallbacks.FilterSave))
        }
    }
}
