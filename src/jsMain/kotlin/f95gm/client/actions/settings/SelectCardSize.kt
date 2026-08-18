package f95gm.client.actions.settings

import f95gm.client.model.settings.CardSize
import f95gm.client.state.runtime.scope
import f95gm.client.state.settings.appSettings
import f95gm.client.state.settings.persistAppSettings
import kotlinx.coroutines.launch

internal fun selectCardSize(value: String) {
    val selected = CardSize.fromValue(value)
    scope.launch { appSettings.enqueue { it.copy(cardSize = selected).also(::persistAppSettings) } }
}
