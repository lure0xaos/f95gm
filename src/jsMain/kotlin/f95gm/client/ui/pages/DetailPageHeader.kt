package f95gm.client.ui.pages

import dev.fritz2.core.RenderContext
import f95gm.client.ui.navigation.*
import f95gm.client.state.runtime.ClientNavigationUiPort

internal fun RenderContext.detailPageHeader(runtime: ClientNavigationUiPort) {
    div("detail-nav navbar border-bottom py-3") {
        navbarBrand()
        jvmConnectionDetails(runtime)
        navbarNavigationLink(NavbarLink.TRACKED_GAMES)
        navbarAccountLinks(runtime)
    }
}
