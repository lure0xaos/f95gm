package f95gm.client.actions.settings

import f95gm.client.model.settings.CardSize
import f95gm.client.state.runtime.ClientSettingsActionsPort
import kotlinx.coroutines.launch

internal fun selectCardSize(runtime: ClientSettingsActionsPort, selected: CardSize) {
    runtime.launchEffect { runtime.settingsStore.update { it.copy(cardSize = selected) } }
}
