package f95gm.server.http.request.ratelimit

@JvmInline
internal value class RequestBudget(val value: Int) {
    init {
        require(value > 0) { "Request budget must be positive" }
    }
}
