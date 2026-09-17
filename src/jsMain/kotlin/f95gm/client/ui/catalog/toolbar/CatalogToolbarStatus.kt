package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.ui.catalog.filters.filterChip
import f95gm.client.ui.catalog.filters.prefixChip
import f95gm.client.model.catalog.FilterSelection
import f95gm.client.ui.catalog.saved.savedFilterManager
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.state.runtime.ClientCatalogUiPort
import f95gm.client.state.runtime.AsyncState
import f95gm.client.model.catalog.asyncState
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.catalogToolbarStatus(runtime: ClientCatalogUiPort) {
    val catalog = runtime.catalogView
    val catalogOptions = runtime.catalogOptionsView
    div("filter-chips d-flex flex-wrap align-items-center gap-2 mt-3") {
        catalog.data.map { it.selectedTags to it.excludedTags }.distinctUntilChanged()
            .render { (included, excluded) ->
                if (included.isNotEmpty() || excluded.isNotEmpty()) {
                    span("chip-label small text-uppercase text-body-secondary fw-semibold") { +UiMessages.app_tags() }
                    included.forEach { filterChip(runtime, it, FilterSelection.INCLUDED) }
                    excluded.forEach { filterChip(runtime, it, FilterSelection.EXCLUDED) }
                }
            }
        catalog.data.map { it.selectedPrefixes to it.excludedPrefixes }.distinctUntilChanged()
            .render { (included, excluded) ->
                if (included.isNotEmpty() || excluded.isNotEmpty()) {
                    span("chip-label small text-uppercase text-body-secondary fw-semibold") { +UiMessages.app_prefixes() }
                    included.forEach { prefixChip(runtime, it, FilterSelection.INCLUDED) }
                    excluded.forEach { prefixChip(runtime, it, FilterSelection.EXCLUDED) }
                }
            }
    }
    catalogOptions.data.map { it.asyncState }.distinctUntilChanged()
        .render { status ->
            if (status is AsyncState.Loading) span("small text-body-secondary d-block mt-3") { +UiMessages.app_loadingChoices() }
            else if (status is AsyncState.Failure) {
                div("small text-danger d-flex flex-wrap align-items-center gap-2 mt-3") {
                    span { +status.message }
                    button(secondaryNavigationButtonClass) {
                        type("button")
                        +UiMessages.app_retry()
                        clicks handledBy {
                            runtime.catalogCommands.retryFilterOptions()
                        }
                    }
                }
            }
        }
    savedFilterManager(runtime)
}
