package f95gm.server.http.request

@JvmInline
internal value class RequestEndpoint(val value: String) {
    init {
        require(value.isNotBlank())
    }

    companion object {
        val DEFAULT = RequestEndpoint("default")
    }
}
