package f95gm.server.config.session

import f95gm.domain.values.F95SessionId
import f95gm.server.config.constants.ServerConstants
import kotlin.uuid.Uuid

internal sealed interface SessionCookieParseResult {
    data class Valid(val id: F95SessionId) : SessionCookieParseResult
    data object Missing : SessionCookieParseResult
    data object Invalid : SessionCookieParseResult
}

internal fun parseSessionCookieResult(header: String?): SessionCookieParseResult {
    val value = header?.split(';')
        ?.asSequence()
        ?.map { it.trim() }
        ?.mapNotNull { pair ->
            val separator = pair.indexOf('=')
            if (separator <= 0) null else pair.substring(0, separator).trim() to pair.substring(separator + 1).trim()
        }
        ?.firstOrNull { it.first == sessionCookie }
        ?.second
        ?.let { it }
        ?: return SessionCookieParseResult.Missing
    if (value.length > ServerConstants.SESSION_ID_LENGTH) return SessionCookieParseResult.Invalid
    return Uuid.parseHexDashOrNull(value)
        ?.let { SessionCookieParseResult.Valid(F95SessionId(it.toString())) }
        ?: SessionCookieParseResult.Invalid
}

internal fun parseSessionCookie(header: String?): F95SessionId? =
    (parseSessionCookieResult(header) as? SessionCookieParseResult.Valid)?.id
