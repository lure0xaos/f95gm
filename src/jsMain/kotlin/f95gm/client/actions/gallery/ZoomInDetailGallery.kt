package f95gm.client.actions.gallery

import f95gm.client.state.config.ClientConstants
import f95gm.client.state.runtime.ClientGalleryActionsPort
import kotlinx.coroutines.launch

internal fun zoomInDetailGallery(runtime: ClientGalleryActionsPort) {
    runtime.launchEffect {
        runtime.updateDetailGallery { state ->
            state.copy(scale = (state.scale + ClientConstants.ZOOM_STEP).coerceAtMost(ClientConstants.MAX_ZOOM))
        }
    }
}
