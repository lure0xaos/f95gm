package f95gm.server.http.auth

import f95gm.server.upstream.parser.htmlText

internal fun isUnauthenticatedUpstreamBody(body: String): Boolean =
    isF95zoneLoginPage(body) || htmlText(body).contains("have to be logged in to access this page", ignoreCase = true)
