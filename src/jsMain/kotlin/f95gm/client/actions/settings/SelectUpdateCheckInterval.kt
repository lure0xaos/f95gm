package f95gm.client.actions.settings

import f95gm.client.model.settings.UpdateCheckInterval
import f95gm.client.state.runtime.scope
import f95gm.client.state.settings.appSettings
import f95gm.client.state.settings.persistAppSettings
import kotlinx.coroutines.launch

internal fun selectUpdateCheckInterval(millis: String) {
    val selected = UpdateCheckInterval.fromMillis(millis.toLongOrNull())
    scope.launch { appSettings.enqueue { it.copy(updateCheckInterval = selected).also(::persistAppSettings) } }
}
