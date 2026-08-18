package f95gm.client.ui.catalog.filters

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.actions.catalog.filters.editing.removeTag
import f95gm.client.model.catalog.TagOption
import f95gm.client.ui.shared.navigation.dangerNavigationButtonClass
import f95gm.client.ui.shared.navigation.navigationButtonClass

internal fun RenderContext.filterChip(option: TagOption, excluded: Boolean) {
    button("filter-chip ${if (excluded) dangerNavigationButtonClass else navigationButtonClass}") {
        type("button")
        +"${if (excluded) "−" else "+"} ${option.name.value}"
        clicks handledBy { removeTag(option.id.value, excluded) }
    }
}
