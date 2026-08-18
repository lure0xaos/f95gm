package f95gm.client.ui.navigation

import dev.fritz2.core.RenderContext
import dev.fritz2.core.alt
import dev.fritz2.core.src
import f95gm.client.routing.navigation.latestUpdatesRoute
import f95gm.client.routing.navigation.router
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.map

internal fun RenderContext.navbarBrand(removeEndMargin: Boolean = false) {
    a("brand navbar-brand${if (removeEndMargin) " me-0" else ""} d-flex align-items-center gap-2 fw-bold") {
        clicks.map { latestUpdatesRoute } handledBy router.navTo
        img("brand-icon rounded-3") {
            attr("width", "40")
            attr("height", "40")
            alt(UiMessages.app_brandName())
            src("favicon.png")
        }
        span("brand-name") { +UiMessages.app_brandShortName() }
    }
}
