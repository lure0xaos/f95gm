package f95gm.client.ui.navigation

import dev.fritz2.core.RenderContext
import dev.fritz2.core.disabled
import dev.fritz2.core.type
import f95gm.client.actions.catalog.loading.reloadCatalog
import f95gm.client.actions.marks.loading.checkMarkedGames
import f95gm.client.state.catalog.catalog
import f95gm.client.state.marks.marks
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.map

internal fun RenderContext.navbarReloadButton(page: NavbarPage) {
    button("page-reload $secondaryNavigationButtonClass px-2") {
        type("button")
        attr("title", UiMessages.app_reload())
        attr("aria-label", UiMessages.app_reload())
        span("bi bi-arrow-clockwise") {}
        if (page == NavbarPage.TRACKED_GAMES || page == NavbarPage.SETTINGS) {
            disabled(marks.data.map { it.loading || it.checking })
            clicks handledBy { checkMarkedGames(showToast = false) }
        } else {
            disabled(catalog.data.map { it.loading })
            clicks handledBy { reloadCatalog() }
        }
    }
}
