package f95gm.client.actions.settings

import f95gm.client.state.runtime.scope
import f95gm.client.state.settings.appSettings
import f95gm.client.state.settings.persistAppSettings
import kotlinx.coroutines.launch

internal fun toggleLazyImageLoading() {
    scope.launch { appSettings.enqueue { it.copy(lazyLoadImages = !it.lazyLoadImages).also(::persistAppSettings) } }
}
