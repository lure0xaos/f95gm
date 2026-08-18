package f95gm.client.ui.shared.pagination

import dev.fritz2.core.RenderContext
import dev.fritz2.core.disabled
import dev.fritz2.core.title
import dev.fritz2.core.type
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass

internal fun RenderContext.pageIconButton(
    iconClass: String,
    label: String,
    disabled: Boolean,
    onClick: () -> Unit
) {
    button("page-arrow $secondaryNavigationButtonClass px-2") {
        type("button")
        attr("aria-label", label)
        title(label)
        disabled(disabled)
        span(iconClass) {}
        clicks handledBy { onClick() }
    }
}
