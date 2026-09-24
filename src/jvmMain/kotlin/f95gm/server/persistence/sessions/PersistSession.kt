package f95gm.server.persistence.sessions

import f95gm.domain.values.F95SessionId
import f95gm.server.config.constants.ServerConstants
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.requireStorageLength
import f95gm.server.persistence.schema.SessionCookiesTable
import f95gm.server.persistence.schema.SessionsTable
import f95gm.server.state.runtime.ServerClock
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update

internal suspend fun persistSession(
    database: DatabaseExecutor,
    clock: ServerClock,
    id: F95SessionId,
    session: UpstreamSession
) {
    val rawId = id.value
    requireStorageLength(rawId, ServerConstants.SESSION_ID_LENGTH, "sessionId")
    requireStorageLength(session.accountKey.value, ServerConstants.ACCOUNT_KEY_LENGTH, "accountKey")
    database.transactionSuspending {
        SessionCookiesTable.deleteWhere { SessionCookiesTable.sessionId eq rawId }
        val updated = SessionsTable.update({ SessionsTable.id eq rawId }) {
            it[SessionsTable.accountKey] = session.accountKey.value.take(ServerConstants.ACCOUNT_KEY_LENGTH)
            it[SessionsTable.updatedAt] = clock.nowMillis()
        }
        if (updated == 0) {
            SessionsTable.insert {
                it[SessionsTable.id] = rawId
                it[SessionsTable.accountKey] = session.accountKey.value.take(ServerConstants.ACCOUNT_KEY_LENGTH)
                it[SessionsTable.updatedAt] = clock.nowMillis()
            }
        }
        session.cookieEntries().forEach { (name, value) ->
            SessionCookiesTable.insert {
                it[SessionCookiesTable.sessionId] = rawId
                it[SessionCookiesTable.name] = name.take(ServerConstants.COOKIE_NAME_LENGTH)
                it[SessionCookiesTable.value] = value.take(ServerConstants.COOKIE_VALUE_LENGTH)
            }
        }
    }
}
