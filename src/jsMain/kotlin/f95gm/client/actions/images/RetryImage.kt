package f95gm.client.actions.images

import f95gm.client.state.images.imageRetry
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun retryImage(source: String) {
    scope.launch {
        imageRetry.enqueue { state ->
            state.copy(
                failedSources = state.failedSources - source,
                attempts = state.attempts + (source to ((state.attempts[source] ?: 0) + 1))
            )
        }
    }
}
