package f95gm.client.ui.shared.content

import dev.fritz2.core.RenderContext
import dev.fritz2.core.id
import f95gm.client.ui.detail.gallery.installDetailImageGallery

internal fun RenderContext.richHtml(className: String, html: String, elementId: String? = null) {
    val content = div(className) {
        if (elementId != null) id(elementId)
    }
    content.domNode.innerHTML = html
    installDetailImageGallery(content.domNode)
}
