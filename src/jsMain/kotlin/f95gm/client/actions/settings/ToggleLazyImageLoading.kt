package f95gm.client.actions.settings

import f95gm.client.state.runtime.ClientSettingsActionsPort
import kotlinx.coroutines.launch

internal fun toggleLazyImageLoading(runtime: ClientSettingsActionsPort) {
    runtime.launchEffect { runtime.settingsStore.update { it.copy(lazyLoadImages = !it.lazyLoadImages) } }
}
