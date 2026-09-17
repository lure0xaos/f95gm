package f95gm.server.upstream.parser

import f95gm.domain.values.F95Html
import f95gm.domain.values.F95OptionName
import f95gm.messages.UiMessages
import f95gm.server.model.detail.DetailSection
import f95gm.server.model.detail.ParsedDetailBody
import f95gm.server.upstream.parser.spoiler.SpoilerContentMode
import f95gm.server.upstream.parser.spoiler.splitSpoilerSections
import f95gm.server.upstream.parser.shared.HtmlBaseUrl
import org.jsoup.Jsoup

internal fun parseDetailBody(
    html: String,
    baseUrl: HtmlBaseUrl
): ParsedDetailBody {
    val body = Jsoup.parseBodyFragment(html, baseUrl.value).body()
    val nodes = body.childNodes().toList()
    val markers = nodes.mapIndexedNotNull { index, node ->
        detailSectionTitle(node)?.let { index to it }
    }
    val firstMarker = markers.firstOrNull()?.first ?: nodes.size
    val overviewBody = Jsoup.parseBodyFragment("", baseUrl.value).body()
    nodes.take(firstMarker).forEach { overviewBody.appendChild(it.clone()) }
    val overview = overviewBody.html()
    val parsedSections = markers.mapIndexedNotNull { markerIndex, (start, title) ->
        val end = markers.getOrNull(markerIndex + 1)?.first ?: nodes.size
        val sectionBody = Jsoup.parseBodyFragment("", baseUrl.value).body()
        nodes.subList(start + 1, end).forEach { sectionBody.appendChild(it.clone()) }
        trimSectionBody(sectionBody)
        sectionBody.html().trim().takeIf { it.isNotBlank() }?.let {
            DetailSection(F95OptionName(title), F95Html(it))
        }
    }
    val overviewParts = splitSpoilerSections(
        DetailSection(F95OptionName(UiMessages.app_overview()), F95Html(overview)),
        contentMode = SpoilerContentMode.KEEP_NON_SPOILER_CONTENT,
        baseUrl = baseUrl
    )
    val normalizedOverview = overviewParts
        .firstOrNull { it.title.value.equals(UiMessages.app_overview(), ignoreCase = true) }
        ?.html?.value
        .orEmpty()
    val overviewSpoilers = overviewParts.filterNot {
        it.title.value.equals(UiMessages.app_overview(), ignoreCase = true)
    }
    val sections = parsedSections.flatMap { section ->
        splitSpoilerSections(
            section,
            contentMode = if (section.title.value.equals("Changelog", ignoreCase = true)) {
                SpoilerContentMode.DISCARD_NON_SPOILER_CONTENT
            } else {
                SpoilerContentMode.KEEP_NON_SPOILER_CONTENT
            },
            baseUrl = baseUrl
        )
    }
    return ParsedDetailBody(normalizedOverview, overviewSpoilers + sections)
}
