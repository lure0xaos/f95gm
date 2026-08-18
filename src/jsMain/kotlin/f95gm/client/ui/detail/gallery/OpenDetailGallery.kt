package f95gm.client.ui.detail.gallery

import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun openDetailGallery(images: List<DetailGalleryImage>, index: Int) {
    scope.launch {
        detailGalleryStore.enqueue {
            DetailGalleryState(
                images = images,
                index = index.coerceIn(0, (images.size - 1).coerceAtLeast(0))
            )
        }
    }
}
