package f95gm.client.ui.catalog.filters

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.actions.catalog.filters.editing.removePrefix
import f95gm.client.model.catalog.PrefixOption
import f95gm.client.ui.shared.navigation.dangerNavigationButtonClass
import f95gm.client.ui.shared.navigation.navigationButtonClass

internal fun RenderContext.prefixChip(option: PrefixOption, excluded: Boolean) {
    button("prefix-chip ${if (excluded) dangerNavigationButtonClass else navigationButtonClass}") {
        type("button")
        +"${if (excluded) "−" else "+"} ${option.name.value}"
        clicks handledBy { removePrefix(option.id.value, excluded) }
    }
}
