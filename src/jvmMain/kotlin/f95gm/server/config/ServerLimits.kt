package f95gm.server.config

/** Defaults grouped by the boundary that owns the limit. */
internal object RequestLimits {
    const val MAX_BODY_BYTES = 1_048_576L
    const val MAX_MEDIA_RESPONSE_BYTES = 8L * 1024L * 1024L
    const val MAX_METRIC_ENDPOINTS = 256
}

internal object UpstreamLimits {
    const val MAX_CONCURRENT_REQUESTS = 32
    const val MAX_METRIC_ENDPOINTS = 64
}

internal object RetryLimits {
    const val UPSTREAM_MAX_TOTAL_DELAY_MILLIS = 60_000L
    const val DATABASE_MAX_TOTAL_DELAY_MILLIS = 2_000L
}

internal object MediaLimits {
    const val DOWNLOAD_BUFFER_BYTES = 16 * 1024
}

internal object RateLimitDefaults {
    const val API_REQUESTS_PER_MINUTE = 120
    const val WINDOW_MILLIS = 60_000L
    const val MAX_BUCKETS = 4_096
}

internal object CatalogLimits {
    const val PAGE_SIZE = 90
    val PAGE_SIZES = setOf(15, 30, 45, 60, 75, 90, 120, 150)
    const val MAX_PAGE = 9_999
    const val MAX_TAGS = 10
    const val MAX_TEXT_LENGTH = 120
}

internal object SessionLimits {
    const val MAX_SESSIONS = 2_000
}

internal object ServerDefaults {
    const val HOST = "127.0.0.1"
    const val PORT = 8080
    const val MIN_PORT = 0
    const val MAX_PORT = 65535
    const val SESSION_MAX_AGE_SECONDS = 60 * 60 * 24 * 30
    const val SESSION_IDLE_MILLIS = 30L * 60L * 1000L
    const val CIRCUIT_BREAKER_OPEN_MILLIS = 30_000L
    const val MAX_CIRCUIT_BREAKER_ENTRIES = 4_096
}
