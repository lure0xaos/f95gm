package f95gm.server.session

import f95gm.domain.values.F95SessionId
import f95gm.server.kernel.MonotonicMillis
import f95gm.server.model.auth.UpstreamSession

/** Persistence-facing session contract that avoids depending on application implementation types. */
internal sealed interface SessionLookup {
    data class Found(val session: UpstreamSession) : SessionLookup
    data object Missing : SessionLookup
}

internal interface SessionRegistryPort {
    fun lookup(id: F95SessionId, now: MonotonicMillis): SessionLookup
    fun put(id: F95SessionId, session: UpstreamSession, now: MonotonicMillis)
}
