package f95gm.server.upstream.parser.spoiler

import f95gm.domain.values.F95Html
import f95gm.domain.values.F95OptionName
import f95gm.server.model.detail.DetailSection
import f95gm.server.upstream.parser.shared.extractNestedSections
import f95gm.server.upstream.parser.shared.HtmlBaseUrl
import f95gm.server.upstream.parser.shared.htmlText
import f95gm.server.upstream.parser.shared.spoilerHeading
import org.jsoup.Jsoup

internal fun splitSpoilerSections(
    section: DetailSection,
    contentMode: SpoilerContentMode,
    baseUrl: HtmlBaseUrl
): List<DetailSection> {
    val body = Jsoup.parseBodyFragment(section.html.value, baseUrl.value).body()
    val extractedSections = extractNestedSections(body, baseUrl)
    val spoilers = body.select("div.bbCodeSpoiler").filter { spoiler ->
        spoiler.parents().none { it.hasClass("bbCodeSpoiler") }
    }
    if (spoilers.isEmpty()) {
        val remainingHtml = body.html().trim()
        val remainingSection = section.copy(html = F95Html(remainingHtml))
        return listOfNotNull(remainingSection.takeIf { htmlText(remainingHtml, baseUrl).isNotBlank() }) + extractedSections
    }

    val elements = body.allElements
    val headings = elements.mapNotNull { element ->
        spoilerHeading(element)?.let { element to it }
    }
    val spoilerTitles = spoilers.map { spoiler ->
        val spoilerIndex = elements.indexOf(spoiler)
        headings.lastOrNull { (heading, _) -> elements.indexOf(heading) < spoilerIndex }?.second
            ?: section.title.value
    }

    val result = mutableListOf<DetailSection>()
    if (contentMode == SpoilerContentMode.KEEP_NON_SPOILER_CONTENT) {
        val remaining = body.clone()
        remaining.select("div.bbCodeSpoiler").remove()
        remaining.select("b").filter { spoilerHeading(it) != null }.forEach { it.remove() }
        val remainingHtml = remaining.html().trim()
        if (remainingHtml.isNotBlank() && htmlText(remainingHtml, baseUrl).isNotBlank()) {
            result += section.copy(html = F95Html(remainingHtml))
        }
    }
    spoilers.forEachIndexed { index, spoiler ->
        result += DetailSection(F95OptionName(spoilerTitles[index]), F95Html(spoiler.outerHtml()))
    }
    return result + extractedSections
}
