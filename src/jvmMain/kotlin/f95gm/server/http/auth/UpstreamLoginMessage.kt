package f95gm.server.http.auth

import org.jsoup.Jsoup

internal fun upstreamLoginMessage(
    body: String,
    baseUrl: String
): String? = Jsoup.parse(body, baseUrl).selectFirst(".blockMessage--error")?.text()
    ?.takeIf { it.isNotBlank() }
