package f95gm.server.http.auth

import f95gm.server.upstream.parser.shared.htmlText
import f95gm.server.upstream.parser.shared.HtmlBaseUrl

internal fun isUnauthenticatedUpstreamBody(
    body: String,
    baseUrl: String
): Boolean = isF95zoneLoginPage(body, baseUrl) ||
    htmlText(body, HtmlBaseUrl(baseUrl)).contains("have to be logged in to access this page", ignoreCase = true)
