package f95gm.server.upstream.media

import f95gm.server.config.upstream.mediaHosts
import f95gm.server.config.upstream.upstream
import java.net.URI

internal fun normalizedMediaSource(rawValue: String): String? {
    val value = when {
        rawValue.trim().startsWith("//") -> "https:${rawValue.trim()}"
        rawValue.trim().startsWith("/") -> upstream + rawValue.trim()
        else -> rawValue.trim()
    }
    val uri = runCatching { URI(value) }.getOrNull()
    return value.takeIf {
        uri?.scheme.equals("https", ignoreCase = true) && uri?.host?.lowercase() in mediaHosts
    }
}
