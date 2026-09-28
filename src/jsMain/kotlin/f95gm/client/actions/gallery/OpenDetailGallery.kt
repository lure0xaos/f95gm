package f95gm.client.actions.gallery

import f95gm.client.state.runtime.ClientGalleryActionsPort
import f95gm.client.model.images.DetailGalleryImage
import f95gm.client.model.images.DetailGalleryState
import kotlinx.coroutines.launch

internal fun openDetailGallery(runtime: ClientGalleryActionsPort, images: List<DetailGalleryImage>, index: Int) {
    runtime.launchEffect {
        runtime.updateDetailGallery {
            DetailGalleryState(
                images = images,
                index = index.coerceIn(0, (images.size - 1).coerceAtLeast(0))
            )
        }
    }
}
