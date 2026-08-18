package f95gm.server.upstream.parser

import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

internal fun trimSectionBody(body: Element) {
    while (true) {
        val node = body.childNodes().firstOrNull() ?: break
        when (node) {
            is TextNode -> {
                val text = node.text()
                val withoutLabelSeparator = text.trimStart().removePrefix(":").trimStart()
                if (withoutLabelSeparator.isBlank()) node.remove()
                else if (withoutLabelSeparator != text) node.text(withoutLabelSeparator)
                else break
            }

            is Element -> if (node.tagName() == "br") node.remove() else break
            else -> break
        }
    }
    while (true) {
        val node = body.childNodes().lastOrNull() ?: break
        when (node) {
            is TextNode -> if (node.text().isBlank()) node.remove() else break
            is Element -> if (node.tagName() == "br") node.remove() else break
            else -> break
        }
    }
}
