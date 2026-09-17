package f95gm.server.upstream.parser

import f95gm.server.upstream.parser.shared.htmlText
import f95gm.server.upstream.parser.shared.HtmlBaseUrl

import f95gm.domain.values.*
import f95gm.messages.UiMessages
import f95gm.server.model.detail.DetailMeta
import f95gm.server.model.detail.ThreadDetail
import f95gm.server.model.ParseResult
import f95gm.server.upstream.html.sanitizeThreadHtml
import f95gm.server.config.upstream.UpstreamBaseUrl
import f95gm.server.upstream.media.proxyMediaUrl
import org.jsoup.Jsoup

internal fun parseThreadDetail(
    threadId: F95ThreadId,
    html: String,
    baseUrl: UpstreamBaseUrl
): ThreadDetail {
    val htmlBaseUrl = HtmlBaseUrl(baseUrl.value)
    val document = Jsoup.parse(html, htmlBaseUrl.value)
    val title = document.selectFirst("meta[property=og:title]")?.attr("content")
        ?.takeIf { it.isNotBlank() }
        ?: document.selectFirst("h1")?.text()
        ?: "${UiMessages.app_untitledItem()} ${threadId.value}"
    val creator = document.selectFirst("a.username")?.text().orEmpty()
    val date = document.selectFirst("time.u-dt")?.text().orEmpty()
    val cover = document.select("[data-src], [data-url]")
        .asSequence()
        .map { it.attr("data-src").ifBlank { it.attr("data-url") } }
        .firstOrNull { it.startsWith("https://", ignoreCase = true) }
        .orEmpty()
    val article = document.selectFirst("article.message-body")
    val body = article?.selectFirst("div.bbWrapper")?.html() ?: article?.html().orEmpty()
    val parsedBody = parseDetailBody(body, htmlBaseUrl)
    val sections = parsedBody.sections
    val meta = listOf(
        "Thread Updated", "Release Date", "Version", "Censored", "OS", "Language", "Length", "Store", "Genre"
    ).mapNotNull { label ->
        sections.firstOrNull { it.title.value.equals(label, ignoreCase = true) }
            ?.html?.value
            ?.let { htmlText(it, htmlBaseUrl) }
            ?.takeIf { it.isNotBlank() }
            ?.let { DetailMeta(F95OptionName(label), F95OptionName(it)) }
    }

    return ThreadDetail(
        threadId = threadId,
        title = F95GameTitle(htmlText(title, htmlBaseUrl)),
        creator = F95DeveloperName(creator),
        date = F95DateLabel(date),
        cover = F95CoverUrl(proxyMediaUrl(cover)),
        meta = meta,
        overviewHtml = F95Html(sanitizeThreadHtml(parsedBody.overview, baseUrl).value),
        sections = sections.map { it.copy(html = F95Html(sanitizeThreadHtml(it.html.value, baseUrl).value)) },
        bodyHtml = F95Html(sanitizeThreadHtml(body, baseUrl).value)
    )
}

internal fun parseThreadDetailResult(
    threadId: F95ThreadId,
    html: String,
    baseUrl: UpstreamBaseUrl
): ParseResult<ThreadDetail> = try {
    ParseResult.Success(parseThreadDetail(threadId, html, baseUrl))
} catch (error: IllegalArgumentException) {
    ParseResult.Invalid(error.message ?: "Invalid thread detail")
}
