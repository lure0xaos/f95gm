package f95gm.client.actions.settings

import f95gm.client.model.settings.AppTheme
import f95gm.client.state.runtime.ClientSettingsActionsPort
import kotlinx.coroutines.launch

internal fun selectAppTheme(runtime: ClientSettingsActionsPort, selected: AppTheme) {
    runtime.launchEffect { runtime.settingsStore.update { it.copy(theme = selected) } }
}
