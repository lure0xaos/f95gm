package f95gm.server.persistence.sessions

import f95gm.domain.values.F95SessionId
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.observability.DatabaseOperation
import f95gm.server.persistence.schema.SessionCookiesTable
import f95gm.server.persistence.schema.SessionsTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere

internal suspend fun removePersistedSession(database: TransactionBoundary, id: F95SessionId) {
    val rawId = id.value
    database.transactionSuspending(DatabaseOperation.SESSIONS_REMOVE) {
        SessionCookiesTable.deleteWhere { SessionCookiesTable.sessionId eq rawId }
        SessionsTable.deleteWhere { SessionsTable.id eq rawId }
    }
}
