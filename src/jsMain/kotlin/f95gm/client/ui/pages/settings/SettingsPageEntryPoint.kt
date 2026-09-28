package f95gm.client.ui.pages.settings

import dev.fritz2.core.RenderContext
import f95gm.client.state.runtime.ClientSettingsUiPort

internal fun RenderContext.settingsPageSections(runtime: ClientSettingsUiPort) {
    renderSettingsPageSections(runtime)
}
