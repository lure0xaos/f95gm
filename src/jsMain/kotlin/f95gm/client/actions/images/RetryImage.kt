package f95gm.client.actions.images

import f95gm.client.state.runtime.ClientImageActionsPort
import f95gm.client.model.images.MediaSource
import kotlinx.coroutines.launch

internal fun retryImage(runtime: ClientImageActionsPort, source: MediaSource) {
    runtime.launchEffect {
        runtime.imageRetry.enqueue { state ->
            state.copy(
                failedSources = state.failedSources - source,
                attempts = state.attempts + (source to ((state.attempts[source] ?: 0) + 1))
            )
        }
    }
}
