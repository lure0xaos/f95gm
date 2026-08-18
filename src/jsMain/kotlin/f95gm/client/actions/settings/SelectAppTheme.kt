package f95gm.client.actions.settings

import f95gm.client.model.settings.AppTheme
import f95gm.client.state.runtime.scope
import f95gm.client.state.settings.appSettings
import f95gm.client.state.settings.persistAppSettings
import kotlinx.coroutines.launch

internal fun selectAppTheme(theme: String) {
    val selected = AppTheme.fromValue(theme)
    scope.launch { appSettings.enqueue { it.copy(theme = selected).also(::persistAppSettings) } }
}
