package f95gm.client.ui.pages.settings

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.model.settings.AppTheme
import f95gm.client.model.settings.CardSize
import f95gm.client.model.settings.PageSize
import f95gm.client.model.settings.UpdateCheckInterval
import f95gm.client.ui.navigation.NavbarPage
import f95gm.client.ui.navigation.responsiveNavbar
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.state.runtime.ClientSettingsPagePort
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.settingsPage(runtime: ClientSettingsPagePort) {
    val appSettings = runtime.appSettingsView
    div("settings-page") {
        responsiveNavbar(
            runtime = runtime,
            navigationId = "settings-navigation",
            page = NavbarPage.SETTINGS,
            toolbar = {}
        )
        settingsPageContent(runtime)
    }
}
