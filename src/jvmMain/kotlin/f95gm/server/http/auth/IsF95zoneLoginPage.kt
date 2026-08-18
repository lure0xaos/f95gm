package f95gm.server.http.auth

import f95gm.server.config.upstream.upstream
import org.jsoup.Jsoup

internal fun isF95zoneLoginPage(body: String): Boolean {
    val document = Jsoup.parse(body, upstream)
    return document.selectFirst("html[data-template=login]") != null ||
            document.title().trim().startsWith("Log in", ignoreCase = true)
}
