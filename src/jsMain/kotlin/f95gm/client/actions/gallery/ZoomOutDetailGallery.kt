package f95gm.client.actions.gallery

import f95gm.client.state.config.ClientConstants
import f95gm.client.state.runtime.ClientGalleryActionsPort
import kotlinx.coroutines.launch

internal fun zoomOutDetailGallery(runtime: ClientGalleryActionsPort) {
    runtime.launchEffect {
        runtime.updateDetailGallery { state ->
            state.copy(scale = (state.scale - ClientConstants.ZOOM_STEP).coerceAtLeast(ClientConstants.MIN_ZOOM))
        }
    }
}
