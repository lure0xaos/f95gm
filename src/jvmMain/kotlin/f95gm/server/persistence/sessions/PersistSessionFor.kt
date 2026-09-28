package f95gm.server.persistence.sessions

import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.kernel.ServerClock
import f95gm.server.session.SessionRegistryPort

internal suspend fun persistSessionFor(
    database: TransactionBoundary,
    registry: SessionRegistryPort,
    clock: ServerClock,
    session: UpstreamSession
) {
    if ((registry.lookup(session.browserId, clock.monotonicMillisTimestamp()) as? f95gm.server.session.SessionLookup.Found)?.session === session) {
        persistSession(database, clock, session.browserId, session)
    }
}
