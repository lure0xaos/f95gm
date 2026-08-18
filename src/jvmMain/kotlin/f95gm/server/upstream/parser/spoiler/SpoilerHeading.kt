package f95gm.server.upstream.parser.spoiler

import f95gm.server.upstream.parser.detailSectionLabels
import org.jsoup.nodes.Element

internal fun spoilerHeading(element: Element): String? {
    if (element.tagName() != "b" || element.parents().any { it.hasClass("bbCodeSpoiler") }) return null
    val value = element.text().trim().removeSuffix(":").trim()
    return if (value.equals("IMPORTANT", ignoreCase = true)) value
    else detailSectionLabels.firstOrNull { it.equals(value, ignoreCase = true) }?.let { value }
}
