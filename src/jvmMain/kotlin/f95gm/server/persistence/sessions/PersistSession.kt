package f95gm.server.persistence.sessions

import f95gm.domain.values.F95SessionId
import f95gm.server.config.constants.ServerStorageLimits
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.observability.DatabaseOperation
import f95gm.server.persistence.database.DatabaseEpochMillis
import f95gm.server.persistence.model.requireStorageLength
import f95gm.server.persistence.schema.SessionCookiesTable
import f95gm.server.persistence.schema.SessionsTable
import f95gm.server.kernel.ServerClock
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update

internal suspend fun persistSession(
    database: TransactionBoundary,
    clock: ServerClock,
    id: F95SessionId,
    session: UpstreamSession
) {
    val rawId = id.value
    requireStorageLength(rawId, ServerStorageLimits.SESSION_ID_LENGTH, "sessionId")
    requireStorageLength(session.accountKey.value, ServerStorageLimits.ACCOUNT_KEY_LENGTH, "accountKey")
    database.transactionSuspending(DatabaseOperation.SESSIONS_PERSIST) {
        val updatedAt = DatabaseEpochMillis(clock.epochMillis().value).value
        SessionCookiesTable.deleteWhere { SessionCookiesTable.sessionId eq rawId }
        val updated = SessionsTable.update({ SessionsTable.id eq rawId }) {
            it[SessionsTable.accountKey] = session.accountKey.value.take(ServerStorageLimits.ACCOUNT_KEY_LENGTH)
            it[SessionsTable.updatedAt] = updatedAt
        }
        if (updated == 0) {
            SessionsTable.insert {
                it[SessionsTable.id] = rawId
                it[SessionsTable.accountKey] = session.accountKey.value.take(ServerStorageLimits.ACCOUNT_KEY_LENGTH)
                it[SessionsTable.updatedAt] = updatedAt
            }
        }
    session.typedCookieEntries().forEach { (name, value) ->
            SessionCookiesTable.insert {
                it[SessionCookiesTable.sessionId] = rawId
                it[SessionCookiesTable.name] = name.value.take(ServerStorageLimits.COOKIE_NAME_LENGTH)
                it[SessionCookiesTable.value] = value.value.take(ServerStorageLimits.COOKIE_VALUE_LENGTH)
            }
        }
    }
}
