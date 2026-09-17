package f95gm.client.state.settings

import f95gm.client.model.settings.AppSettingsState
import f95gm.client.state.runtime.BrowserStorage
import dev.fritz2.core.RootStore

internal class ClientSettingsStore(
    private val appSettings: RootStore<AppSettingsState>,
    private val storage: BrowserStorage
) {
    suspend fun update(transform: (AppSettingsState) -> AppSettingsState) {
        appSettings.enqueue { current ->
            transform(current).also { persistAppSettings(it, storage) }
        }
    }
}
