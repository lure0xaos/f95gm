package f95gm.client.ui.detail.gallery

internal data class DetailGalleryState(
    val images: List<DetailGalleryImage> = emptyList(),
    val index: Int = 0,
    val scale: Double = 1.0
) {
    val current: DetailGalleryImage?
        get() = images.getOrNull(index)
}
