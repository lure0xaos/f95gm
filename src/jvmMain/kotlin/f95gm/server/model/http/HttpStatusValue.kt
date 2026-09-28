package f95gm.server.model.http

@JvmInline
internal value class HttpStatusValue(val value: Int) {
    init { require(value in 100..599) { "HTTP status must be between 100 and 599" } }
}
