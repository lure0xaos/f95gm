package f95gm.server.persistence.sessions

import f95gm.server.persistence.database.dbQuery
import f95gm.server.persistence.schema.SessionCookiesTable
import f95gm.server.persistence.schema.SessionsTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere

internal fun removePersistedSession(id: String) {
    dbQuery {
        SessionCookiesTable.deleteWhere { SessionCookiesTable.sessionId eq id }
        SessionsTable.deleteWhere { SessionsTable.id eq id }
    }
}
