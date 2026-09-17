package f95gm.client.model.runtime

internal enum class HttpStatusClass {
    AUTHENTICATION_REQUIRED,
    RESPONDING,
    UNAVAILABLE,
    ERROR;

    companion object {
        fun from(status: HttpStatusCodeValue): HttpStatusClass = when {
            status.value == 401 -> AUTHENTICATION_REQUIRED
            status.value in 200..399 -> RESPONDING
            status.value >= 500 -> UNAVAILABLE
            else -> ERROR
        }
    }
}
