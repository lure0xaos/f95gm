package f95gm.client.ui.detail.gallery

import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import f95gm.client.state.runtime.ClientGalleryUiPort
import f95gm.client.model.images.MediaSource
import f95gm.client.model.images.DetailGalleryImage

internal fun installDetailImageGallery(runtime: ClientGalleryUiPort, root: HTMLElement) {
    val links = root.querySelectorAll("a")
    val images = mutableListOf<DetailGalleryImage>()
    for (index in 0 until links.length) {
        val link = links.item(index) as? HTMLAnchorElement ?: continue
        val image = link.querySelector("img") as? HTMLImageElement ?: continue
        val source = link.getAttribute("href")?.takeIf { it.isNotBlank() } ?: image.src
        images += DetailGalleryImage(MediaSource(source), image.alt)
    }
    if (images.isEmpty()) return
    for (index in 0 until links.length) {
        val link = links.item(index) as? HTMLAnchorElement ?: continue
        if (link.querySelector("img") == null) continue
        link.addEventListener("click", { event ->
            event.preventDefault()
        runtime.galleryCommands.openDetailGallery(images, images.indexOfFirst { it.source.value == link.getAttribute("href") }.coerceAtLeast(0))
        })
    }
}
