package f95gm.client.state.runtime

import f95gm.client.model.images.MediaSource
import f95gm.client.actions.images.imageLoadFailed as imageLoadFailedFromImages
import f95gm.client.actions.images.retryImage as retryImageFromImages

/** UI commands expose intent without writable stores or transport access. */
internal interface ClientImagesCommands {
    fun imageLoadFailed(source: MediaSource)
    fun retryImage(source: MediaSource)
}

internal class ClientImagesCommandsAdapter(private val actions: ClientImageActionsPort) : ClientImagesCommands {
    override fun imageLoadFailed(source: MediaSource) = imageLoadFailedFromImages(actions, source)
    override fun retryImage(source: MediaSource) = retryImageFromImages(actions, source)
}
