package f95gm.client.ui.pages.settings

import dev.fritz2.core.RenderContext
import f95gm.messages.UiMessages

internal fun RenderContext.settingsPageHeader() {
    h1("h3 mb-1") { +UiMessages.app_settings() }
    p("text-body-secondary mb-4") { +UiMessages.app_settingsHint() }
}
