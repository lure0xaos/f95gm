package f95gm.server.model

import kotlin.time.Duration

@JvmInline
internal value class DatabasePath(val value: String) {
    init { require(value.isNotBlank()) { "Database path must not be blank" } }
}

@JvmInline
internal value class RequestDuration(val value: Duration) {
    init { require(!value.isNegative()) { "Request duration must not be negative" } }
    val millis: Long get() = value.inWholeMilliseconds
}

@JvmInline
internal value class RetryAttempt(val value: Int) {
    init { require(value >= 0) { "Retry attempt must not be negative" } }
}

@JvmInline
internal value class RetryDelay(val value: Duration) {
    init { require(!value.isNegative()) { "Retry delay must not be negative" } }
}
