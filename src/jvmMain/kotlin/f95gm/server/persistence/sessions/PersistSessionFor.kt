package f95gm.server.persistence.sessions

import f95gm.server.model.auth.UpstreamSession
import f95gm.server.state.runtime.sessions

internal fun persistSessionFor(session: UpstreamSession) {
    sessions.entries.firstOrNull { it.value === session }?.let { (id, stored) ->
        persistSession(id, stored)
    }
}
