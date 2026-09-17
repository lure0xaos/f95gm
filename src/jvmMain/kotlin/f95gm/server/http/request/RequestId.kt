package f95gm.server.http.request

@JvmInline
internal value class RequestId(val value: String) {
    init {
        require(value.length in 1..64 && value.all { it.isLetterOrDigit() || it in "-_" })
    }

    companion object {
        fun parseOrNull(value: String?): RequestId? = value?.takeIf { isValid(it) }?.let(::RequestId)
        private fun isValid(value: String): Boolean = value.length in 1..64 && value.all { it.isLetterOrDigit() || it in "-_" }
    }
}
