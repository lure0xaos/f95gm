package f95gm.server.config

import f95gm.server.config.constants.FilterLimits

internal sealed interface ConfigKey<T> {
    val name: String
    val propertyName: ConfigPropertyName get() = ConfigPropertyName(name)
    val environmentName: ConfigurationEnvironmentName get() = configurationEnvironmentName(propertyName)
    val default: T
    val codec: ConfigCodec<T>
    val sensitive: Boolean get() = false
    fun encodedDefault(): String = codec.encode(default)
}

internal interface ConfigCodec<T> { fun encode(value: T): String }
internal object TextCodec : ConfigCodec<String> { override fun encode(value: String) = value }
internal object IntCodec : ConfigCodec<Int> { override fun encode(value: Int) = value.toString() }
internal object LongCodec : ConfigCodec<Long> { override fun encode(value: Long) = value.toString() }
internal object DurationCodec : ConfigCodec<Millis> { override fun encode(value: Millis) = value.value.toString() }
internal object ByteSizeCodec : ConfigCodec<Bytes> { override fun encode(value: Bytes) = value.value.toString() }
internal object BooleanCodec : ConfigCodec<Boolean> { override fun encode(value: Boolean) = value.toString() }

internal data class TextKey(override val name: String, override val default: String, override val sensitive: Boolean = false) : ConfigKey<String> { override val codec = TextCodec }
internal data class IntKey(override val name: String, override val default: Int) : ConfigKey<Int> { override val codec = IntCodec }
internal data class LongKey(override val name: String, override val default: Long) : ConfigKey<Long> { override val codec = LongCodec }
internal data class DurationKey(override val name: String, override val default: Millis) : ConfigKey<Millis> { override val codec = DurationCodec }
internal data class ByteSizeKey(override val name: String, override val default: Bytes) : ConfigKey<Bytes> { override val codec = ByteSizeCodec }
internal data class BooleanKey(override val name: String, override val default: Boolean) : ConfigKey<Boolean> { override val codec = BooleanCodec }

/** Central declaration of supported configuration properties and their defaults. */
internal object Keys {
    val HOST = TextKey("f95gm.host", ServerDefaults.HOST)
    val PORT = IntKey("f95gm.port", ServerDefaults.PORT)
    val DATABASE = TextKey("f95gm.database", "")
    val STATIC_DIRECTORY = TextKey("f95gm.staticDir", "")
    val REQUEST_TIMEOUT = DurationKey("f95gm.requestTimeoutMillis", Millis(30_000L))
    val CONNECT_TIMEOUT = DurationKey("f95gm.connectTimeoutMillis", Millis(10_000L))
    val SOCKET_TIMEOUT = DurationKey("f95gm.socketTimeoutMillis", Millis(30_000L))
    val SHUTDOWN_TIMEOUT = DurationKey("f95gm.shutdownTimeoutMillis", Millis(10_000L))
    val SESSION_IDLE = DurationKey("f95gm.sessionIdleMillis", Millis(ServerDefaults.SESSION_IDLE_MILLIS))
    val MAX_SESSIONS = IntKey("f95gm.maxSessions", SessionLimits.MAX_SESSIONS)
    val MAX_REQUEST_BODY = ByteSizeKey("f95gm.maxRequestBodyBytes", Bytes(RequestLimits.MAX_BODY_BYTES))
    val MAX_METRIC_ENDPOINTS = IntKey("f95gm.maxMetricEndpoints", RequestLimits.MAX_METRIC_ENDPOINTS)
    val MAX_UPSTREAM_METRIC_ENDPOINTS =
        IntKey("f95gm.maxUpstreamMetricEndpoints", UpstreamLimits.MAX_METRIC_ENDPOINTS)
    val MAX_UPSTREAM_CONCURRENT_REQUESTS = IntKey(
        "f95gm.maxUpstreamConcurrentRequests", UpstreamLimits.MAX_CONCURRENT_REQUESTS
    )
    val MAX_MEDIA_RESPONSE_BYTES = ByteSizeKey("f95gm.maxMediaResponseBytes", Bytes(RequestLimits.MAX_MEDIA_RESPONSE_BYTES))
    val MEDIA_DOWNLOAD_BUFFER_BYTES = IntKey("f95gm.mediaDownloadBufferBytes", MediaLimits.DOWNLOAD_BUFFER_BYTES)
    val MAX_CATALOG_PAGE = IntKey("f95gm.maxCatalogPage", CatalogLimits.MAX_PAGE)
    val MAX_CATALOG_TAGS = IntKey("f95gm.maxCatalogTags", CatalogLimits.MAX_TAGS)
    val MAX_CATALOG_TEXT_LENGTH = IntKey("f95gm.maxCatalogTextLength", CatalogLimits.MAX_TEXT_LENGTH)
    val MAX_IMPORTED_QUICK_LINKS = IntKey("f95gm.maxImportedQuickLinks", FilterLimits.MAX_QUICK_LINKS)
    val MAX_FILTER_ID_LENGTH = IntKey("f95gm.maxFilterIdLength", FilterLimits.FILTER_ID_LENGTH)
    val API_REQUESTS_PER_MINUTE = IntKey("f95gm.apiRequestsPerMinute", RateLimitDefaults.API_REQUESTS_PER_MINUTE)
    val ACCOUNT_REQUESTS_PER_MINUTE =
        IntKey("f95gm.accountRequestsPerMinute", RateLimitDefaults.API_REQUESTS_PER_MINUTE / 2)
    val RATE_LIMIT_WINDOW = DurationKey("f95gm.rateLimitWindowMillis", Millis(RateLimitDefaults.WINDOW_MILLIS))
    val MAX_RATE_LIMIT_BUCKETS = IntKey("f95gm.maxRateLimitBuckets", RateLimitDefaults.MAX_BUCKETS)
    val CIRCUIT_BREAKER_OPEN = DurationKey("f95gm.circuitBreakerOpenMillis", Millis(ServerDefaults.CIRCUIT_BREAKER_OPEN_MILLIS))
    val MAX_CIRCUIT_BREAKER_ENTRIES =
        IntKey("f95gm.maxCircuitBreakerEntries", ServerDefaults.MAX_CIRCUIT_BREAKER_ENTRIES)
    val RETRY_MAX_ATTEMPTS = IntKey("f95gm.retryMaxAttempts", 3)
    val RETRY_BASE_DELAY = DurationKey("f95gm.retryBaseDelayMillis", Millis(250L))
    val RETRY_MAX_DELAY = DurationKey("f95gm.retryMaxDelayMillis", Millis(30_000L))
    val RETRY_MAX_TOTAL_DELAY = DurationKey(
        "f95gm.retryMaxTotalDelayMillis", Millis(RetryLimits.UPSTREAM_MAX_TOTAL_DELAY_MILLIS)
    )
    val DATABASE_RETRY_MAX_TOTAL_DELAY = DurationKey(
        "f95gm.databaseRetryMaxTotalDelayMillis", Millis(RetryLimits.DATABASE_MAX_TOTAL_DELAY_MILLIS)
    )
    val DEPLOYMENT_MODE = TextKey("f95gm.deploymentMode", DeploymentMode.SINGLE_INSTANCE.name)
    val OPEN_BROWSER = BooleanKey("f95gm.openBrowser", true)
    val UPSTREAM_BASE_URL = TextKey("f95gm.upstreamBaseUrl", "https://f95zone.to")

    val all: List<ConfigKey<*>> = listOf(
        HOST, PORT, DATABASE, STATIC_DIRECTORY, REQUEST_TIMEOUT, CONNECT_TIMEOUT, SOCKET_TIMEOUT, SHUTDOWN_TIMEOUT,
        SESSION_IDLE, MAX_SESSIONS, MAX_REQUEST_BODY, MAX_METRIC_ENDPOINTS, MAX_UPSTREAM_METRIC_ENDPOINTS,
        MAX_UPSTREAM_CONCURRENT_REQUESTS, MAX_MEDIA_RESPONSE_BYTES,
        MEDIA_DOWNLOAD_BUFFER_BYTES, MAX_CATALOG_PAGE, MAX_CATALOG_TAGS, MAX_CATALOG_TEXT_LENGTH,
        MAX_IMPORTED_QUICK_LINKS, MAX_FILTER_ID_LENGTH, API_REQUESTS_PER_MINUTE,
        ACCOUNT_REQUESTS_PER_MINUTE, RATE_LIMIT_WINDOW, MAX_RATE_LIMIT_BUCKETS,
        CIRCUIT_BREAKER_OPEN, MAX_CIRCUIT_BREAKER_ENTRIES, RETRY_MAX_ATTEMPTS,
        RETRY_BASE_DELAY, RETRY_MAX_DELAY, RETRY_MAX_TOTAL_DELAY, DATABASE_RETRY_MAX_TOTAL_DELAY,
        DEPLOYMENT_MODE, OPEN_BROWSER, UPSTREAM_BASE_URL
    )

    val sensitive: Set<ConfigPropertyName> = all.filter { it.sensitive }.mapTo(mutableSetOf()) { it.propertyName }
}
