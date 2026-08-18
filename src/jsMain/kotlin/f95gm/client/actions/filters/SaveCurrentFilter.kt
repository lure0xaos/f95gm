package f95gm.client.actions.filters

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.postJson
import f95gm.client.data.filters.parseSavedFilters
import f95gm.client.data.filters.savedFilterBody
import f95gm.client.model.filters.SavedFiltersState
import f95gm.client.state.catalog.catalog
import f95gm.client.state.filters.savedFilterName
import f95gm.client.state.filters.savedFilters
import f95gm.client.state.runtime.scope
import f95gm.domain.api.F95Api
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun saveCurrentFilter() {
    scope.launch {
        val name = savedFilterName.data.first().trim()
        if (name.isBlank()) {
            showToast(UiMessages.app_savedFilterNameRequired())
            return@launch
        }
        runCatching {
            postJson(F95Api.FILTERS, savedFilterBody(name, catalog.data.first()))
        }.onSuccess { payload ->
            val saved = parseSavedFilters(payload).firstOrNull()
            if (saved == null) {
                showToast(UiMessages.app_savedFilterResponseInvalid())
                return@onSuccess
            }
            savedFilters.enqueue { state ->
                SavedFiltersState(
                    items = listOf(saved) + state.items.filterNot {
                        it.name.value.equals(
                            saved.name.value,
                            ignoreCase = true
                        )
                    }
                )
            }
            savedFilterName.enqueue { "" }
            showToast(UiMessages.app_savedQuickLink(saved.name.value))
        }.onFailure { error ->
            showToast(error.message ?: UiMessages.app_filterSaveFailed())
        }
    }
}
