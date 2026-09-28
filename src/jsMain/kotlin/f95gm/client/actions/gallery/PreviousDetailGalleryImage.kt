package f95gm.client.actions.gallery

import f95gm.client.state.runtime.ClientGalleryActionsPort
import kotlinx.coroutines.launch

internal fun previousDetailGalleryImage(runtime: ClientGalleryActionsPort) {
    runtime.launchEffect {
        runtime.updateDetailGallery { state ->
            if (state.images.size < 2) state
            else state.copy(
                index = (state.index - 1 + state.images.size) % state.images.size,
                scale = 1.0
            )
        }
    }
}
