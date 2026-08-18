package f95gm.server.model.auth

import f95gm.domain.values.F95AccountKey

internal data class UpstreamSession(
    val accountKey: F95AccountKey,
    val cookies: MutableMap<String, String> = linkedMapOf()
) {
    fun header(): String = cookies.entries.joinToString("; ") { (name, value) -> "$name=$value" }
}
