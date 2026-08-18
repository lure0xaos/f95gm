package f95gm.client.ui.detail.gallery

import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun previousDetailGalleryImage() {
    scope.launch {
        detailGalleryStore.enqueue { state ->
            if (state.images.size < 2) state
            else state.copy(
                index = (state.index - 1 + state.images.size) % state.images.size,
                scale = 1.0
            )
        }
    }
}
