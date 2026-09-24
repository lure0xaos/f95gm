package f95gm.server.http.request

@JvmInline
internal value class BrowserTabId(val value: String) {
    init {
        require(value.length in 1..128 && value.all { it.isLetterOrDigit() || it in "-_" })
    }
}
