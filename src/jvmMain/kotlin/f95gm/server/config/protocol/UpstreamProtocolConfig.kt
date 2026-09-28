package f95gm.server.config.protocol

/** Stable upstream protocol values shared by the request adapters. */
internal object UpstreamProtocolConfig {
    const val LOGIN_PAGE = "/login/"
    const val LOGIN = "/login/login"
    const val SESSION_COOKIE = "xf_session"
    const val MAX_REDIRECTS = 4
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36"
    const val X_REQUESTED_WITH = "X-Requested-With"
    const val XML_HTTP_REQUEST = "XMLHttpRequest"
    const val ACCEPT_CATALOG_JSON = "application/json, text/javascript, */*; q=0.01"
    const val ACCEPT_HTML_DOCUMENT = "text/html,application/xhtml+xml"
    const val ACCEPT_MEDIA = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
    const val CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded"
    const val ACCEPT_LANGUAGE_ENGLISH = "en-US,en;q=0.9"
    const val SEC_FETCH_SITE = "Sec-Fetch-Site"
    const val SEC_FETCH_MODE = "Sec-Fetch-Mode"
    const val SEC_FETCH_USER = "Sec-Fetch-User"
    const val SEC_FETCH_DEST = "Sec-Fetch-Dest"
}
