package f95gm.server.persistence.sessions

import f95gm.domain.values.F95AccountKey
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.session.sessionMaxAgeSeconds
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.dbQuery
import f95gm.server.persistence.schema.SessionCookiesTable
import f95gm.server.persistence.schema.SessionsTable
import f95gm.server.state.runtime.sessions
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

internal fun restoreSessions() {
    val now = System.currentTimeMillis()
    dbQuery {
        SessionsTable.selectAll().forEach { row ->
            val id = row[SessionsTable.id]
            val updatedAt = row[SessionsTable.updatedAt]
            if (row[SessionsTable.accountKey].isBlank() ||
                updatedAt <= 0L ||
                now - updatedAt > sessionMaxAgeSeconds * ServerConstants.MILLIS_PER_SECOND
            ) {
                SessionCookiesTable.deleteWhere { SessionCookiesTable.sessionId eq id }
                SessionsTable.deleteWhere { SessionsTable.id eq id }
            } else {
                val cookies = SessionCookiesTable.selectAll()
                    .where { SessionCookiesTable.sessionId eq id }
                    .associate { cookie -> cookie[SessionCookiesTable.name] to cookie[SessionCookiesTable.value] }
                sessions[id] = UpstreamSession(
                    accountKey = F95AccountKey(row[SessionsTable.accountKey]),
                    cookies = cookies.toMutableMap()
                )
            }
        }
    }
}
