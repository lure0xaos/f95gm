package f95gm.client.ui.pages.settings

import dev.fritz2.core.RenderContext
import f95gm.client.ui.navigation.NavbarPage
import f95gm.client.ui.navigation.responsiveNavbar
import f95gm.client.state.runtime.ClientSettingsUiPort

internal fun RenderContext.settingsPageContent(runtime: ClientSettingsUiPort) {
    div("settings-content container-xl py-4") {
        settingsPageHeader()
        settingsPageSections(runtime)
        }
}
