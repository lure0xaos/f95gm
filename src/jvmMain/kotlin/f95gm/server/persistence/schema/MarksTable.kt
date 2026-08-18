package f95gm.server.persistence.schema

import f95gm.domain.tracking.GameTrackingState
import f95gm.server.config.constants.ServerConstants
import org.jetbrains.exposed.v1.core.Table

internal object MarksTable : Table("marks") {
    val accountKey = varchar("account_key", ServerConstants.ACCOUNT_KEY_LENGTH)
    val threadId = varchar("thread_id", ServerConstants.THREAD_ID_LENGTH)
    val trackingState =
        enumerationByName("tracking_state", ServerConstants.ENUMERATION_LENGTH, GameTrackingState::class)
    val storedVersion = varchar("stored_version", ServerConstants.STORED_VERSION_LENGTH)
    val lastSeenUpdateVersion = varchar("last_seen_update_version", ServerConstants.STORED_VERSION_LENGTH).default("")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(accountKey, threadId)
}
