package f95gm.client.actions.settings

import f95gm.client.model.settings.UpdateCheckInterval
import f95gm.client.state.runtime.ClientSettingsActionsPort
import kotlinx.coroutines.launch

internal fun selectUpdateCheckInterval(runtime: ClientSettingsActionsPort, selected: UpdateCheckInterval) {
    runtime.launchEffect { runtime.settingsStore.update { it.copy(updateCheckInterval = selected) } }
}
