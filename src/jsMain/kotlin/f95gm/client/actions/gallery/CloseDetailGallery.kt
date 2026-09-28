package f95gm.client.actions.gallery

import f95gm.client.state.runtime.ClientGalleryActionsPort
import f95gm.client.model.images.DetailGalleryState
import kotlinx.coroutines.launch

internal fun closeDetailGallery(runtime: ClientGalleryActionsPort) {
    runtime.launchEffect { runtime.updateDetailGallery { DetailGalleryState() } }
}
