package f95gm.client.ui.detail.gallery

import dev.fritz2.core.RenderContext
import dev.fritz2.core.Tag
import dev.fritz2.core.type

internal fun RenderContext.galleryButton(
    label: String,
    ariaLabel: String,
    action: () -> Unit,
    additionalClass: String = "",
    configure: Tag<*>.() -> Unit = {}
) {
    button("gallery-button btn btn-light btn-lg shadow $additionalClass") {
        type("button")
        attr("aria-label", ariaLabel)
        configure()
        clicks handledBy { action() }
        +label
    }
}
