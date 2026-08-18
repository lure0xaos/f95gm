package f95gm.client.ui.detail.gallery

import f95gm.client.state.config.ClientConstants
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun zoomOutDetailGallery() {
    scope.launch {
        detailGalleryStore.enqueue { state ->
            state.copy(scale = (state.scale - ClientConstants.ZOOM_STEP).coerceAtLeast(ClientConstants.MIN_ZOOM))
        }
    }
}
