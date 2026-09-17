package f95gm.server.upstream.parser.shared

import org.jsoup.Jsoup

internal fun htmlText(value: String, baseUrl: HtmlBaseUrl): String =
    Jsoup.parseBodyFragment(value, baseUrl.value).text().trim()
