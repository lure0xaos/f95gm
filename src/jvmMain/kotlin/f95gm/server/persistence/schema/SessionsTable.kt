package f95gm.server.persistence.schema

import f95gm.server.config.constants.ServerStorageLimits
import org.jetbrains.exposed.v1.core.Table

internal object SessionsTable : Table("sessions") {
    val id = varchar("id", ServerStorageLimits.SESSION_ID_LENGTH)
    val accountKey = varchar("account_key", ServerStorageLimits.ACCOUNT_KEY_LENGTH)
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
