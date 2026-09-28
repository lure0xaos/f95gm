package f95gm.server.observability

@JvmInline
internal value class RetryAttempt(val value: Int) {
    init { require(value > 0) }
}

@JvmInline
internal value class RequestDurationMillis(val value: Long) {
    init { require(value >= 0L) }
}
