package f95gm.client.actions.images

import f95gm.client.state.runtime.ClientImageActionsPort
import f95gm.client.model.images.MediaSource
import kotlinx.coroutines.launch

internal fun imageLoadFailed(runtime: ClientImageActionsPort, source: MediaSource) {
    runtime.launchEffect {
        runtime.imageRetry.enqueue { state ->
            state.copy(failedSources = state.failedSources + source)
        }
    }
}
