package f95gm.server.persistence.sessions

import f95gm.server.config.constants.ServerConstants
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.dbQuery
import f95gm.server.persistence.schema.SessionCookiesTable
import f95gm.server.persistence.schema.SessionsTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert

internal fun persistSession(id: String, session: UpstreamSession) {
    dbQuery {
        SessionCookiesTable.deleteWhere { SessionCookiesTable.sessionId eq id }
        SessionsTable.deleteWhere { SessionsTable.id eq id }
        SessionsTable.insert {
            it[SessionsTable.id] = id
            it[SessionsTable.accountKey] = session.accountKey.value.take(ServerConstants.ACCOUNT_KEY_LENGTH)
            it[SessionsTable.updatedAt] = System.currentTimeMillis()
        }
        session.cookies.forEach { (name, value) ->
            SessionCookiesTable.insert {
                it[SessionCookiesTable.sessionId] = id
                it[SessionCookiesTable.name] = name.take(ServerConstants.COOKIE_NAME_LENGTH)
                it[SessionCookiesTable.value] = value.take(ServerConstants.COOKIE_VALUE_LENGTH)
            }
        }
    }
}
