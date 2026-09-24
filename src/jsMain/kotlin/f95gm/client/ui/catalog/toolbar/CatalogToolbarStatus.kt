package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.actions.catalog.loading.loadFilterOptionsIfNeeded
import f95gm.client.state.catalog.catalog
import f95gm.client.state.catalog.catalogOptions
import f95gm.client.state.catalog.loadedOptionsCategory
import f95gm.client.ui.catalog.filters.filterChip
import f95gm.client.ui.catalog.filters.prefixChip
import f95gm.client.ui.catalog.saved.savedFilterManager
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.catalogToolbarStatus() {
    div("filter-chips d-flex flex-wrap align-items-center gap-2 mt-3") {
        catalog.data.map { it.selectedTags to it.excludedTags }.distinctUntilChanged()
            .render { (included, excluded) ->
                if (included.isNotEmpty() || excluded.isNotEmpty()) {
                    span("chip-label small text-uppercase text-body-secondary fw-semibold") { +UiMessages.app_tags() }
                    included.forEach { filterChip(it, false) }
                    excluded.forEach { filterChip(it, true) }
                }
            }
        catalog.data.map { it.selectedPrefixes to it.excludedPrefixes }.distinctUntilChanged()
            .render { (included, excluded) ->
                if (included.isNotEmpty() || excluded.isNotEmpty()) {
                    span("chip-label small text-uppercase text-body-secondary fw-semibold") { +UiMessages.app_prefixes() }
                    included.forEach { prefixChip(it, false) }
                    excluded.forEach { prefixChip(it, true) }
                }
            }
    }
    catalogOptions.data.map { it.loading to it.error }.distinctUntilChanged()
        .render { (loading, error) ->
            if (loading) span("small text-body-secondary d-block mt-3") { +UiMessages.app_loadingChoices() }
            else if (error.isNotBlank()) {
                div("small text-danger d-flex flex-wrap align-items-center gap-2 mt-3") {
                    span { +error }
                    button(secondaryNavigationButtonClass) {
                        type("button")
                        +UiMessages.app_retry()
                        clicks handledBy {
                            loadedOptionsCategory = null
                            loadFilterOptionsIfNeeded()
                        }
                    }
                }
            }
        }
    savedFilterManager()
}
