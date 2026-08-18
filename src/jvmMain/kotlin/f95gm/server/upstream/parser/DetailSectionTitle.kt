package f95gm.server.upstream.parser

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

internal fun detailSectionTitle(node: Node): String? {
    val element = node as? Element ?: return null
    if (element.tagName() != "b") return null
    val value = element.text().trim().removeSuffix(":").trim()
    return detailSectionLabels.firstOrNull { it.equals(value, ignoreCase = true) }?.let { value }
}
