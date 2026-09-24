package f95gm.server.persistence.sessions

import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.state.runtime.ServerClock
import f95gm.server.state.runtime.session.SessionRegistry

internal suspend fun persistSessionFor(
    database: DatabaseExecutor,
    registry: SessionRegistry,
    clock: ServerClock,
    session: UpstreamSession
) {
    if (registry.get(session.browserId) === session) persistSession(database, clock, session.browserId, session)
}
