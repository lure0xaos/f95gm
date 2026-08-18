package f95gm.client.ui.navigation

import dev.fritz2.core.RenderContext
import dev.fritz2.core.id
import dev.fritz2.core.type
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.messages.UiMessages

internal fun RenderContext.responsiveNavbar(
    navigationId: String,
    catalogLink: Boolean,
    toolbar: RenderContext.() -> Unit,
    trailingAction: RenderContext.() -> Unit = {},
    leadingAccountAction: RenderContext.() -> Unit = {}
) {
    div("navbar-shell navbar navbar-expand-xl border-bottom sticky-top bg-black py-2") {
        div("navbar-wrap w-100") {
            div("navbar-inner container-fluid px-0") {
                div("navbar-row d-flex flex-wrap align-items-center w-100 gap-2 px-2") {
                    navbarBrand(removeEndMargin = true)
                    jvmConnectionDetails()
                    navbarReloadButton(myGamesPage = catalogLink)
                    navbarNavigationLink(catalogLink)
                    button("navbar-toggle $secondaryNavigationButtonClass navbar-toggler d-inline-flex d-xl-none") {
                        type("button")
                        attr("data-bs-toggle", "collapse")
                        attr("data-bs-target", "#$navigationId")
                        attr("aria-controls", navigationId)
                        attr("aria-expanded", "false")
                        attr("aria-label", UiMessages.app_toggleNavigation())
                        span("bi bi-list") {}
                        span("d-none d-sm-inline") { +UiMessages.app_menu() }
                    }
                    div("navbar-menu collapse navbar-collapse") {
                        id(navigationId)
                        toolbar()
                    }
                    navbarAccountLinks(leadingAccountAction)
                    trailingAction()
                }
            }
        }
    }
}
