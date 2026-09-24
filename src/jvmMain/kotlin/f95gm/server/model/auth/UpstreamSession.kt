package f95gm.server.model.auth

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.config.constants.ServerConstants

internal class UpstreamSession(
    val browserId: F95SessionId,
    val accountKey: F95AccountKey,
    cookies: Map<String, String> = emptyMap()
) {
    private val cookieValues = LinkedHashMap<String, String>()
    private val cookieLock = Any()

    init {
        cookies.forEach { (name, value) -> updateCookie(name, value) }
    }

    fun header(): String = synchronized(cookieLock) {
        cookieValues.entries.joinToString("; ") { (name, value) -> "$name=$value" }
    }

    fun updateCookie(name: String, value: String): Boolean = synchronized(cookieLock) {
        if (name.isBlank() || name.length > ServerConstants.COOKIE_NAME_LENGTH ||
            value.length > ServerConstants.COOKIE_VALUE_LENGTH
        ) return@synchronized false
        val currentBytes = cookieValues.entries.sumOf { it.key.length + it.value.length }
        val oldBytes = cookieValues[name]?.let { name.length + it.length } ?: 0
        val newBytes = currentBytes - oldBytes + name.length + value.length
        if (name !in cookieValues && cookieValues.size >= ServerConstants.MAX_UPSTREAM_COOKIE_COUNT) return@synchronized false
        if (newBytes > ServerConstants.MAX_UPSTREAM_COOKIE_BYTES) return@synchronized false
        cookieValues[name] = value
        true
    }

    fun cookieEntries(): Map<String, String> = synchronized(cookieLock) { cookieValues.toMap() }
}
