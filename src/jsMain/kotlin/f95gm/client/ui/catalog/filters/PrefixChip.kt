package f95gm.client.ui.catalog.filters

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.model.catalog.FilterSelection
import f95gm.client.model.catalog.PrefixOption
import f95gm.client.ui.shared.navigation.dangerNavigationButtonClass
import f95gm.client.ui.shared.navigation.navigationButtonClass
import f95gm.client.state.runtime.ClientCatalogUiPort

internal fun RenderContext.prefixChip(runtime: ClientCatalogUiPort, option: PrefixOption, selection: FilterSelection) {
    val excluded = selection == FilterSelection.EXCLUDED
    button("prefix-chip ${if (excluded) dangerNavigationButtonClass else navigationButtonClass}") {
        type("button")
        +"${if (excluded) "−" else "+"} ${option.name.value}"
        clicks handledBy {
            runtime.catalogCommands.removePrefix(option.id.value,
                selection
            )
        }
    }
}
