package f95gm.server.upstream.parser

import f95gm.domain.values.F95Html
import f95gm.domain.values.F95OptionName
import f95gm.server.config.upstream.upstream
import f95gm.server.model.detail.DetailSection
import f95gm.server.upstream.parser.spoiler.spoilerHeading
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal fun extractNestedSections(body: Element): List<DetailSection> {
    val downloadHeading = body.allElements
        .firstOrNull { spoilerHeading(it)?.equals("DOWNLOAD", ignoreCase = true) == true }
        ?: return emptyList()
    val downloadBody = Jsoup.parseBodyFragment("", upstream).body()
    var sibling = downloadHeading.nextSibling()
    while (sibling != null) {
        val next = sibling.nextSibling()
        downloadBody.appendChild(sibling.clone())
        sibling.remove()
        sibling = next
    }
    downloadHeading.remove()
    val imageSection = extractTrailingImages(downloadBody)
    val html = downloadBody.html().trim()
    return buildList {
        if (htmlText(html).isNotBlank()) {
            add(DetailSection(F95OptionName("DOWNLOAD"), F95Html(html)))
        }
        if (imageSection != null) add(imageSection)
    }
}
