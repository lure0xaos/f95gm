package f95gm.server.persistence.sessions

import f95gm.domain.values.F95SessionId
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.schema.SessionCookiesTable
import f95gm.server.persistence.schema.SessionsTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere

internal suspend fun removePersistedSession(database: DatabaseExecutor, id: F95SessionId) {
    val rawId = id.value
    database.transactionSuspending {
        SessionCookiesTable.deleteWhere { SessionCookiesTable.sessionId eq rawId }
        SessionsTable.deleteWhere { SessionsTable.id eq rawId }
    }
}
