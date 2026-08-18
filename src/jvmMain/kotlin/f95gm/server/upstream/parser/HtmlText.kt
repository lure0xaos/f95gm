package f95gm.server.upstream.parser

import f95gm.server.config.upstream.upstream
import org.jsoup.Jsoup

internal fun htmlText(value: String): String = Jsoup.parseBodyFragment(value, upstream).text().trim()
