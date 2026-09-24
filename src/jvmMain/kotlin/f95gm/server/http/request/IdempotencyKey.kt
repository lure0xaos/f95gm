package f95gm.server.http.request

@JvmInline
internal value class IdempotencyKey(val value: String) {
    init {
        require(value.length in 1..128 && value.all { it.isLetterOrDigit() || it in "-_." })
    }
}

internal fun validIdempotencyKey(raw: String?): IdempotencyKey? =
    raw?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { IdempotencyKey(it) }.getOrNull() }
