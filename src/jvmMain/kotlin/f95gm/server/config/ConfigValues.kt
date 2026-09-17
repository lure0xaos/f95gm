package f95gm.server.config

import java.nio.file.Path
import kotlin.io.path.isDirectory

@JvmInline
internal value class ServerPort(val value: Int) {
    init { require(value in ServerDefaults.MIN_PORT..ServerDefaults.MAX_PORT) { "Port is out of range: $value" } }
}

@JvmInline
internal value class SessionCapacity(val value: Int) {
    init { require(value > 0) { "Session capacity must be positive" } }
}

@JvmInline
internal value class UpstreamConcurrencyLimit(val value: Int) {
    init { require(value > 0) { "Upstream concurrency must be positive" } }
}

@JvmInline
internal value class RequestsPerMinute(val value: Int) {
    init { require(value > 0) { "Requests per minute must be positive" } }
}

@JvmInline
internal value class RateLimitBucketCount(val value: Int) {
    init { require(value > 0) { "Rate-limit bucket count must be positive" } }
}

@JvmInline
internal value class CircuitBreakerEntryCount(val value: Int) {
    init { require(value > 0) { "Circuit-breaker entry count must be positive" } }
}

@JvmInline
internal value class MediaDownloadBufferBytes(val value: Int) {
    init { require(value > 0) { "Media download buffer size must be positive" } }
}

internal data class CatalogPageLimit(val value: Int) {
    init { require(value > 0) { "Catalog page limit must be positive" } }
}

internal data class CatalogTagLimit(val value: Int) {
    init { require(value > 0) { "Catalog tag limit must be positive" } }
}

internal data class CatalogTextLength(val value: Int) {
    init { require(value > 0) { "Catalog text length must be positive" } }
}

@JvmInline
internal value class ImportedQuickLinkLimit(val value: Int) {
    init { require(value > 0) { "Imported quick-link limit must be positive" } }
}

@JvmInline
internal value class FilterIdLength(val value: Int) {
    init { require(value > 0) { "Filter ID length must be positive" } }
}

@JvmInline
internal value class ServerHost(val value: String) {
    init { require(value.isNotBlank()) { "Host must not be blank" } }
}

@JvmInline
internal value class DatabaseLocation(val value: String) {
    init { require(value.isBlank() || value == value.trim()) { "Database location must not have surrounding whitespace" } }
}

@JvmInline
internal value class StaticDirectoryPath(val value: String) {
    val path: Path get() = kotlin.io.path.Path(value)

    init { require(value.isNotBlank() && value == value.trim()) { "Static directory path must be non-blank and trimmed: $value" } }
}
