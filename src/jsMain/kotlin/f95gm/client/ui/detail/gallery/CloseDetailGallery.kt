package f95gm.client.ui.detail.gallery

import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun closeDetailGallery() {
    scope.launch { detailGalleryStore.enqueue { DetailGalleryState() } }
}
