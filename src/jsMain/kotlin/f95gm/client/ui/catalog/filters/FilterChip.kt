package f95gm.client.ui.catalog.filters

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.model.catalog.FilterSelection
import f95gm.client.model.catalog.TagOption
import f95gm.client.ui.shared.navigation.dangerNavigationButtonClass
import f95gm.client.ui.shared.navigation.navigationButtonClass
import f95gm.client.state.runtime.ClientCatalogUiPort

internal fun RenderContext.filterChip(runtime: ClientCatalogUiPort, option: TagOption, selection: FilterSelection) {
    val excluded = selection == FilterSelection.EXCLUDED
    button("filter-chip ${if (excluded) dangerNavigationButtonClass else navigationButtonClass}") {
        type("button")
        +"${if (excluded) "−" else "+"} ${option.name.value}"
        clicks handledBy {
            runtime.catalogCommands.removeTag(option.id.value,
                selection
            )
        }
    }
}
