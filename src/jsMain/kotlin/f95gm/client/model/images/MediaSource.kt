package f95gm.client.model.images

internal value class MediaSource(val value: String) {
    fun withRetry(attempt: Int): String = when {
        attempt <= 0 -> value
        '?' in value -> "$value&retry=$attempt"
        else -> "$value?retry=$attempt"
    }
}
