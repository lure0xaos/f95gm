package f95gm.server.persistence.schema

import f95gm.server.config.constants.ServerStorageLimits
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

internal object SessionCookiesTable : Table("session_cookies") {
    val sessionId = varchar("session_id", ServerStorageLimits.SESSION_ID_LENGTH)
        .references(SessionsTable.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", ServerStorageLimits.COOKIE_NAME_LENGTH)
    val value = varchar("value", ServerStorageLimits.COOKIE_VALUE_LENGTH)

    override val primaryKey = PrimaryKey(sessionId, name)

    init {
        index("session_cookies_name_idx", false, name)
    }
}
