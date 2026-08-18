package f95gm.server.http.auth

import f95gm.server.config.upstream.upstream
import org.jsoup.Jsoup

internal fun upstreamLoginMessage(body: String): String? =
    Jsoup.parse(body, upstream).selectFirst(".blockMessage--error")?.text()?.takeIf { it.isNotBlank() }
