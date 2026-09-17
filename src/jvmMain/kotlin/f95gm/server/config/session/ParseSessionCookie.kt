package f95gm.server.config.session

import f95gm.domain.values.F95SessionId
import f95gm.server.config.constants.ServerStorageLimits
import f95gm.server.config.protocol.SessionProtocolConfig
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
        ?.firstOrNull { it.first == SessionProtocolConfig.BROWSER_COOKIE }
        ?.second
        ?.let { it }
        ?: return SessionCookieParseResult.Missing
    if (value.length > ServerStorageLimits.SESSION_ID_LENGTH) return SessionCookieParseResult.Invalid
    return Uuid.parseHexDashOrNull(value)
        ?.let { SessionCookieParseResult.Valid(F95SessionId(it.toString())) }
        ?: SessionCookieParseResult.Invalid
}
