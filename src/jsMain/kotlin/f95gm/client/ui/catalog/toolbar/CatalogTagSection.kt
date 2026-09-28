package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.*
import f95gm.client.state.dropdown.DropdownSearchKeys
import f95gm.client.ui.catalog.filters.filterPicker
import f95gm.client.state.runtime.ClientCatalogUiPort
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.map

internal fun RenderContext.catalogTagSection(runtime: ClientCatalogUiPort) {
    val catalog = runtime.catalogView
    val catalogOptions = runtime.catalogOptionsView
    catalogFilterCard(UiMessages.app_tags()) {
        filterPicker(
            runtime = runtime,
            fieldLabel = UiMessages.app_includeTags(),
            options = catalogOptions.data.map { it.tags },
            selected = catalog.data.map { it.tagPicker },
            highlighted = catalog.data.map { state ->
                state.selectedTags.map { it.id.value }.toSet()
            },
            searchKey = DropdownSearchKeys.INCLUDE_TAGS,
            onChange = { runtime.catalogCommands.selectTagPicker(it) }
        )
        filterPicker(
            runtime = runtime,
            fieldLabel = UiMessages.app_excludeTags(),
            options = catalogOptions.data.map { it.tags },
            selected = catalog.data.map { it.excludedTagPicker },
            highlighted = catalog.data.map { state ->
                state.excludedTags.map { it.id.value }.toSet()
            },
            searchKey = DropdownSearchKeys.EXCLUDE_TAGS,
            onChange = { runtime.catalogCommands.selectExcludedTagPicker(it) }
        )
    }
}
