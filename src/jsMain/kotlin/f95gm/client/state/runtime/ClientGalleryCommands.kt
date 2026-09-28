package f95gm.client.state.runtime

import f95gm.client.model.images.DetailGalleryImage

/** UI commands expose intent without writable stores or transport access. */
internal interface ClientGalleryCommands {
    fun closeDetailGallery()
    fun nextDetailGalleryImage()
    fun openDetailGallery(images: List<DetailGalleryImage>, index: Int)
    fun previousDetailGalleryImage()
    fun zoomInDetailGallery()
    fun zoomOutDetailGallery()
}

internal class ClientGalleryCommandsAdapter(private val actions: ClientGalleryActionsPort) : ClientGalleryCommands {
    override fun closeDetailGallery() = f95gm.client.actions.gallery.closeDetailGallery(actions)
    override fun nextDetailGalleryImage() = f95gm.client.actions.gallery.nextDetailGalleryImage(actions)
    override fun openDetailGallery(images: List<DetailGalleryImage>, index: Int) = f95gm.client.actions.gallery.openDetailGallery(actions, images, index)
    override fun previousDetailGalleryImage() = f95gm.client.actions.gallery.previousDetailGalleryImage(actions)
    override fun zoomInDetailGallery() = f95gm.client.actions.gallery.zoomInDetailGallery(actions)
    override fun zoomOutDetailGallery() = f95gm.client.actions.gallery.zoomOutDetailGallery(actions)
}
