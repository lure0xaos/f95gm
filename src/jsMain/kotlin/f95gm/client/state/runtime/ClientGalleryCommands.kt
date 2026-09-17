package f95gm.client.state.runtime

import f95gm.client.model.images.DetailGalleryImage
import f95gm.client.actions.gallery.closeDetailGallery as closeDetailGalleryFromGallery
import f95gm.client.actions.gallery.nextDetailGalleryImage as nextDetailGalleryImageFromGallery
import f95gm.client.actions.gallery.openDetailGallery as openDetailGalleryFromGallery
import f95gm.client.actions.gallery.previousDetailGalleryImage as previousDetailGalleryImageFromGallery
import f95gm.client.actions.gallery.zoomInDetailGallery as zoomInDetailGalleryFromGallery
import f95gm.client.actions.gallery.zoomOutDetailGallery as zoomOutDetailGalleryFromGallery

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
    override fun closeDetailGallery() = closeDetailGalleryFromGallery(actions)
    override fun nextDetailGalleryImage() = nextDetailGalleryImageFromGallery(actions)
    override fun openDetailGallery(images: List<DetailGalleryImage>, index: Int) = openDetailGalleryFromGallery(actions, images, index)
    override fun previousDetailGalleryImage() = previousDetailGalleryImageFromGallery(actions)
    override fun zoomInDetailGallery() = zoomInDetailGalleryFromGallery(actions)
    override fun zoomOutDetailGallery() = zoomOutDetailGalleryFromGallery(actions)
}
