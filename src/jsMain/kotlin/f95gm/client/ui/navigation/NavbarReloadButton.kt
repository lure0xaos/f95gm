package f95gm.client.ui.navigation

import dev.fritz2.core.RenderContext
import dev.fritz2.core.disabled
import dev.fritz2.core.type
import f95gm.client.actions.marks.loading.MarkCheckNotification
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.state.runtime.ClientNavbarReloadPort
import f95gm.client.model.marks.loading
import f95gm.client.model.marks.checking
import f95gm.client.model.catalog.loading
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.map

internal fun RenderContext.navbarReloadButton(runtime: ClientNavbarReloadPort, page: NavbarPage) {
    val catalog = runtime.catalogView
    val marks = runtime.marksView
    button("page-reload $secondaryNavigationButtonClass px-2") {
        type("button")
        attr("title", UiMessages.app_reload())
        attr("aria-label", UiMessages.app_reload())
        span("bi bi-arrow-clockwise") {}
        if (page == NavbarPage.TRACKED_GAMES || page == NavbarPage.SETTINGS) {
            disabled(marks.data.map { it.loading || it.checking })
            clicks handledBy { runtime.marksCommands.checkMarkedGames(MarkCheckNotification.SILENT) }
        } else {
            disabled(catalog.data.map { it.loading })
            clicks handledBy { runtime.catalogCommands.reloadCatalog() }
        }
    }
}
