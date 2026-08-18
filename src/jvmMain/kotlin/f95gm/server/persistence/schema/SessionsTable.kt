package f95gm.server.persistence.schema

import f95gm.server.config.constants.ServerConstants
import org.jetbrains.exposed.v1.core.Table

internal object SessionsTable : Table("sessions") {
    val id = varchar("id", ServerConstants.SESSION_ID_LENGTH)
    val accountKey = varchar("account_key", ServerConstants.ACCOUNT_KEY_LENGTH)
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
