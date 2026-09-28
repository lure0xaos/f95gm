package f95gm.server.model.auth

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.config.constants.ServerStorageLimits
import f95gm.server.kernel.ResourceLock

internal class UpstreamSession(
    val browserId: F95SessionId,
    val accountKey: F95AccountKey,
    cookies: Map<String, String> = emptyMap()
) {
    @JvmInline
    internal value class CookieName(val value: String)

    @JvmInline
    internal value class CookieValue(val value: String)

    internal data class CookieSnapshot(
        val header: String,
        val names: Set<CookieName>,
        val entries: Map<CookieName, CookieValue>
    )
    private val cookieValues = LinkedHashMap<CookieName, CookieValue>()
    private val cookieLock = ResourceLock()

    init {
        cookies.forEach { (name, value) -> updateCookie(name, value) }
    }

    fun header(): String = cookieSnapshot().header

    fun cookieSnapshot(): CookieSnapshot = cookieLock.withLock {
        CookieSnapshot(
            header = cookieValues.entries.joinToString("; ") { (name, value) -> "${name.value}=${value.value}" },
            names = cookieValues.keys.toSet(),
            entries = cookieValues.toMap()
        )
    }

    fun cookieNames(): Set<CookieName> = cookieSnapshot().names

    fun updateCookie(name: String, value: String): Boolean = updateCookie(CookieName(name), CookieValue(value))

    private fun updateCookie(name: CookieName, value: CookieValue): Boolean = cookieLock.withLock {
        if (!isSafeCookieName(name.value)) return@withLock false
        if (value.value.length > ServerStorageLimits.COOKIE_VALUE_LENGTH) return@withLock false
        if (value.value.any { it.isISOControl() || it == ';' || it == ',' }) return@withLock false
        val currentBytes = cookieValues.entries.sumOf { it.key.value.length + it.value.value.length }
        val oldBytes = cookieValues[name]?.let { name.value.length + it.value.length } ?: 0
        val newBytes = currentBytes - oldBytes + name.value.length + value.value.length
        if (name !in cookieValues && cookieValues.size >= ServerStorageLimits.MAX_UPSTREAM_COOKIE_COUNT) return@withLock false
        if (newBytes > ServerStorageLimits.MAX_UPSTREAM_COOKIE_BYTES) return@withLock false
        cookieValues[name] = value
        true
    }

    private fun isSafeCookieName(name: String): Boolean =
        name.isNotBlank() && name.length <= ServerStorageLimits.COOKIE_NAME_LENGTH &&
            name.all { it.isAsciiCookieTokenCharacter() }

    private fun Char.isAsciiCookieTokenCharacter(): Boolean =
        this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' ||
            this in "!#$%&'*+-.^_`|~"

    internal fun typedCookieEntries(): Map<CookieName, CookieValue> = cookieSnapshot().entries

    companion object {
        /** Restores cookies from the persistence wire format at the persistence boundary. */
        internal fun fromPersistedCookies(
            browserId: F95SessionId,
            accountKey: F95AccountKey,
            cookies: Map<String, String>
        ): UpstreamSession = UpstreamSession(browserId, accountKey, cookies)
    }
}
