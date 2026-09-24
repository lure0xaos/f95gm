package f95gm.server.config.constants

import io.ktor.http.*
import java.net.URI

internal object ServerConstants {
    val UPSTREAM: String = configuredUpstreamBase()
    val LATEST_PAGE: String = "$UPSTREAM/sam/latest_alpha/"
    val LATEST_API: String = "${LATEST_PAGE}latest_data.php"
    const val UPSTREAM_LOGIN_PAGE = "/login/"
    const val UPSTREAM_LOGIN = "/login/login"
    const val UPSTREAM_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36"
    const val SESSION_COOKIE = "f95gm_session"
    const val SHUTDOWN_COOKIE = "f95gm_shutdown"
    const val REQUEST_ID_HEADER = "X-Request-ID"
    const val RATE_LIMIT_HEADER = "X-RateLimit-Limit"
    const val RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining"
    const val RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset"
    const val RATE_LIMIT_IP_LIMIT_HEADER = "X-RateLimit-IP-Limit"
    const val RATE_LIMIT_IP_REMAINING_HEADER = "X-RateLimit-IP-Remaining"
    const val RATE_LIMIT_IP_RESET_HEADER = "X-RateLimit-IP-Reset"
    const val RATE_LIMIT_ACCOUNT_LIMIT_HEADER = "X-RateLimit-Account-Limit"
    const val RATE_LIMIT_ACCOUNT_REMAINING_HEADER = "X-RateLimit-Account-Remaining"
    const val RATE_LIMIT_ACCOUNT_RESET_HEADER = "X-RateLimit-Account-Reset"
    const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
    const val UPSTREAM_SESSION_COOKIE = "xf_session"
    const val DATABASE_PROPERTY = "f95gm.database"
    const val MAX_REQUEST_BODY_BYTES = 1_048_576L
    const val MAX_MEDIA_RESPONSE_BYTES = 8L * 1024L * 1024L
    const val API_REQUESTS_PER_MINUTE = 120
    const val RATE_LIMIT_WINDOW_MILLIS = 60_000L
    const val MAX_RATE_LIMIT_BUCKETS = 4_096
    const val CIRCUIT_BREAKER_OPEN_MILLIS = 30_000L
    const val MAX_CIRCUIT_BREAKER_ENTRIES = 4_096
    const val DEFAULT_HOST = "127.0.0.1"
    const val DEFAULT_PORT = 8080
    const val MIN_PORT = 0
    const val MAX_PORT = 65535
    const val SESSION_MAX_AGE_SECONDS = 60 * 60 * 24 * 30
    const val SESSION_IDLE_MILLIS = 30L * 60L * 1000L
    const val MAX_SESSIONS = 2_000
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
    val STATUS_BAD_REQUEST = ServerHttpStatus.BadRequest
    val STATUS_OK = ServerHttpStatus.Ok
    val STATUS_UNAUTHORIZED = ServerHttpStatus.Unauthorized
    val STATUS_FORBIDDEN = ServerHttpStatus.Forbidden
    val STATUS_NOT_FOUND = ServerHttpStatus.NotFound
    val STATUS_BAD_GATEWAY = ServerHttpStatus.BadGateway
    val STATUS_ACCEPTED = HttpStatusCode.Accepted
    val STATUS_CONFLICT = HttpStatusCode.Conflict
    val STATUS_PAYLOAD_TOO_LARGE = HttpStatusCode.PayloadTooLarge
    val STATUS_TOO_MANY_REQUESTS = HttpStatusCode.TooManyRequests
    val STATUS_SERVICE_UNAVAILABLE = HttpStatusCode.ServiceUnavailable
    val STATUS_GATEWAY_TIMEOUT = HttpStatusCode.GatewayTimeout
    val STATUS_UNSUPPORTED_MEDIA_TYPE = HttpStatusCode.UnsupportedMediaType
    const val SESSION_ID_LENGTH = 128
    const val ACCOUNT_KEY_LENGTH = 320
    const val COOKIE_NAME_LENGTH = 128
    const val COOKIE_VALUE_LENGTH = 4096
    const val MAX_UPSTREAM_COOKIE_COUNT = 64
    const val MAX_UPSTREAM_COOKIE_BYTES = 32 * 1024
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

private fun configuredUpstreamBase(): String {
    val configured = System.getProperty("f95gm.upstreamBaseUrl")?.trim()?.trimEnd('/')
        ?.takeIf(String::isNotEmpty) ?: "https://f95zone.to"
    val uri = runCatching { URI(configured) }.getOrElse {
        throw IllegalArgumentException(
            "Invalid F95zone upstream base URL",
            it
        )
    }
    val localHttp = uri.scheme.equals("http", ignoreCase = true) &&
            uri.host?.lowercase() in setOf("localhost", "127.0.0.1", "::1", "[::1]")
    require(uri.isAbsolute && uri.host != null && uri.scheme.equals("https", ignoreCase = true) || localHttp) {
        "F95GM upstream overrides must use HTTPS (HTTP is permitted only for loopback test fixtures)"
    }
    require(uri.userInfo == null && uri.query == null && uri.fragment == null && (uri.path.isNullOrEmpty() || uri.path == "/")) {
        "F95GM upstream base URL must be an origin without credentials, query, or fragment"
    }
    return "${uri.scheme.lowercase()}://${uri.rawAuthority}"
}
