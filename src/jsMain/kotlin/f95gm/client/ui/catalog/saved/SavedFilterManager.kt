package f95gm.client.ui.catalog.saved

import dev.fritz2.core.*
import f95gm.client.actions.filters.deleteSavedFilter
import f95gm.client.actions.filters.retrySavedFilters
import f95gm.client.actions.filters.saveCurrentFilter
import f95gm.client.actions.filters.updateSavedFilterName
import f95gm.client.state.filters.savedFilterName
import f95gm.client.state.filters.savedFilters
import f95gm.client.ui.shared.navigation.dangerNavigationButtonClass
import f95gm.client.ui.shared.navigation.successNavigationButtonClass
import f95gm.client.ui.shared.textFieldClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.savedFilterManager() {
    div("saved-filters border-top pt-3 mt-4") {
        div("saved-heading d-flex flex-wrap align-items-baseline justify-content-between gap-2") {
            div("saved-title text-uppercase text-primary small fw-semibold") { +UiMessages.app_savedQuickLinks() }
            p("saved-hint small text-body-secondary mb-0") { +UiMessages.app_savedLinksHint() }
        }
        div("saved-form input-group mt-3") {
            input("saved-input $textFieldClass") {
                type("text")
                placeholder(UiMessages.app_filterNamePlaceholder())
                value(savedFilterName.data)
                inputs.values() handledBy { updateSavedFilterName(it) }
            }
            button("saved-add $successNavigationButtonClass") {
                type("button")
                span("bi bi-link-45deg me-1") {}
                +UiMessages.app_saveQuickLink()
                clicks handledBy { saveCurrentFilter() }
            }
        }
        savedFilters.data.map { it.items to it.loading }.distinctUntilChanged().render { (items, loading) ->
            when {
                loading -> span("saved-loading small text-body-secondary d-block mt-2") { +UiMessages.app_loadingSavedFilters() }
                items.isEmpty() -> span("saved-empty small text-body-secondary d-block mt-2") { +UiMessages.app_noSavedFilters() }
                else -> {
                    div("saved-list row g-2 mt-3") {
                        items.forEach { filter ->
                            div("saved-item col-auto") {
                                div("saved-link input-group input-group-sm w-auto") {
                                    span("saved-name input-group-text text-nowrap") {
                                        span("bi bi-link-45deg me-1") {}
                                        +filter.name.value
                                    }
                                    button("saved-remove $dangerNavigationButtonClass") {
                                        type("button")
                                        span("bi bi-trash") {}
                                        attr("aria-label", UiMessages.app_remove())
                                        title(UiMessages.app_remove())
                                        clicks handledBy { deleteSavedFilter(filter.id.value) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        savedFilters.data.map { it.error }.distinctUntilChanged().render { error ->
            if (error.isNotBlank()) {
                div("saved-error small text-danger d-flex align-items-center gap-2 mt-2") {
                    span { +error }
                    button("btn btn-sm btn-outline-danger") {
                        type("button")
                        +UiMessages.app_retry()
                        clicks handledBy { retrySavedFilters() }
                    }
                }
            }
        }
    }
}
