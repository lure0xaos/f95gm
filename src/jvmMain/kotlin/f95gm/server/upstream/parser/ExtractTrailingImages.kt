package f95gm.server.upstream.parser

import f95gm.domain.values.F95Html
import f95gm.domain.values.F95OptionName
import f95gm.messages.UiMessages
import f95gm.server.config.upstream.upstream
import f95gm.server.model.detail.DetailSection
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

internal fun extractTrailingImages(body: Element): DetailSection? {
    val firstImageContainer = body.children().firstOrNull { child ->
        child.tagName().equals("img", ignoreCase = true) || child.selectFirst("img") != null
    } ?: return null
    val imagesBody = Jsoup.parseBodyFragment("", upstream).body()
    var node: Node? = firstImageContainer
    while (node != null) {
        val next = node.nextSibling()
        imagesBody.appendChild(node.clone())
        node.remove()
        node = next
    }
    val html = imagesBody.html().trim()
    return if (imagesBody.select("img").isNotEmpty()) {
        DetailSection(F95OptionName(UiMessages.app_images()), F95Html(html))
    } else {
        null
    }
}
