package f95gm.server.http.static

import java.nio.file.InvalidPathException
import java.nio.file.Path

internal sealed interface StaticPathPolicy {
    data class Accepted(val path: StaticResourcePath) : StaticPathPolicy
    data object Rejected : StaticPathPolicy
}

internal fun safeStaticPathDecision(requestedPath: String): StaticPathPolicy {
    val requested = requestedPath.ifBlank { "index.html" }
    if (requested.any { it == '\u0000' }) return StaticPathPolicy.Rejected
    if (requested.contains("%2e", ignoreCase = true) || requested.contains("%2f", ignoreCase = true) ||
        requested.contains("%5c", ignoreCase = true)
    ) return StaticPathPolicy.Rejected
    val normalized = try {
        Path.of(requested).normalize()
    } catch (_: InvalidPathException) {
        return StaticPathPolicy.Rejected
    } catch (_: SecurityException) {
        return StaticPathPolicy.Rejected
    }
    if (normalized.isAbsolute || normalized.startsWith("..")) return StaticPathPolicy.Rejected
    return StaticPathPolicy.Accepted(StaticResourcePath(normalized.toString().replace('\\', '/')))
}

internal fun safeStaticPath(requestedPath: String): StaticResourcePath? =
    (safeStaticPathDecision(requestedPath) as? StaticPathPolicy.Accepted)?.path
