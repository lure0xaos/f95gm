package f95gm.client.ui.detail.gallery

import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement

internal fun installDetailImageGallery(root: HTMLElement) {
    val links = root.querySelectorAll("a")
    val images = mutableListOf<DetailGalleryImage>()
    for (index in 0 until links.length) {
        val link = links.item(index) as? HTMLAnchorElement ?: continue
        val image = link.querySelector("img") as? HTMLImageElement ?: continue
        val source = link.getAttribute("href")?.takeIf { it.isNotBlank() } ?: image.src
        images += DetailGalleryImage(source, image.alt)
    }
    if (images.isEmpty()) return
    for (index in 0 until links.length) {
        val link = links.item(index) as? HTMLAnchorElement ?: continue
        if (link.querySelector("img") == null) continue
        link.addEventListener("click", { event ->
            event.preventDefault()
            openDetailGallery(images, images.indexOfFirst { it.source == link.getAttribute("href") }.coerceAtLeast(0))
        })
    }
}
