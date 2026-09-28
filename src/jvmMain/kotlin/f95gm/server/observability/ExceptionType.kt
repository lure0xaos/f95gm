package f95gm.server.observability

@JvmInline
internal value class ExceptionType(val value: String) {
    init { require(value.isNotBlank()) }
}
