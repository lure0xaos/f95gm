package f95gm.server.config.constants

internal object ServerConstants {
    const val UPSTREAM = "https://f95zone.to"
    const val LATEST_PAGE = "$UPSTREAM/sam/latest_alpha/"
    const val LATEST_API = "$LATEST_PAGE/latest_data.php"
    const val UPSTREAM_LOGIN_PAGE = "/login/"
    const val UPSTREAM_LOGIN = "/login/login"
    const val UPSTREAM_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36"
    const val SESSION_COOKIE = "f95gm_session"
    const val SHUTDOWN_COOKIE = "f95gm_shutdown"
    const val UPSTREAM_SESSION_COOKIE = "xf_session"
    const val DATABASE_PROPERTY = "f95gm.database"
    const val DEFAULT_HOST = "127.0.0.1"
    const val DEFAULT_PORT = 8080
    const val MIN_PORT = 0
    const val MAX_PORT = 65535
    const val SESSION_MAX_AGE_SECONDS = 60 * 60 * 24 * 30
    const val MILLIS_PER_SECOND = 1000L
    const val API_PAGE_SIZE = 90
    val API_PAGE_SIZES = setOf(15, 30, 45, 60, 75, 90, 120, 150)
    const val MAX_PAGE = 9999
    const val MAX_TAGS = 10
    const val MAX_TEXT_LENGTH = 120
    const val MAX_FILTER_OPTIONS = 20
    const val MAX_QUICK_LINKS = 500
    const val REDIRECT_STATUS_MIN = 300
    const val REDIRECT_STATUS_MAX = 399
    const val STATUS_BAD_REQUEST = 400
    const val STATUS_UNAUTHORIZED = 401
    const val STATUS_FORBIDDEN = 403
    const val STATUS_NOT_FOUND = 404
    const val STATUS_BAD_GATEWAY = 502
    const val SESSION_ID_LENGTH = 128
    const val ACCOUNT_KEY_LENGTH = 320
    const val COOKIE_NAME_LENGTH = 128
    const val COOKIE_VALUE_LENGTH = 4096
    const val THREAD_ID_LENGTH = 64
    const val TITLE_LENGTH = 512
    const val STORED_VERSION_LENGTH = 128
    const val DEVELOPER_LENGTH = 256
    const val COVER_LENGTH = 2048
    const val FILTER_ID_LENGTH = 64
    const val FILTER_NAME_LENGTH = 100
    const val OPTION_ID_LENGTH = 40
    const val OPTION_NAME_LENGTH = 160
    const val ENUMERATION_LENGTH = 32
    const val SHORT_ENUMERATION_LENGTH = 16
    const val MAX_UPSTREAM_REDIRECTS = 4
}
