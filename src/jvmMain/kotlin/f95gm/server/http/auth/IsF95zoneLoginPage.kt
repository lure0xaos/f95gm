package f95gm.server.http.auth

import org.jsoup.Jsoup

internal fun isF95zoneLoginPage(
    body: String,
    baseUrl: String
): Boolean {
    val document = Jsoup.parse(body, baseUrl)
    return document.selectFirst("html[data-template=login]") != null ||
            document.title().trim().startsWith("Log in", ignoreCase = true)
}
