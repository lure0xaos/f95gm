package f95gm.server.persistence.schema

import f95gm.domain.tracking.GameTrackingState
import org.jetbrains.exposed.v1.core.Table

internal object MarksTable : Table("marks") {
    val accountKey = varchar("account_key", f95gm.server.config.constants.ServerStorageLimits.ACCOUNT_KEY_LENGTH)
    val threadId = varchar("thread_id", f95gm.server.config.constants.ServerStorageLimits.THREAD_ID_LENGTH)
    val trackingState =
        enumerationByName("tracking_state", f95gm.server.config.constants.FilterLimits.ENUMERATION_LENGTH, GameTrackingState::class)
    val storedVersion = varchar("stored_version", f95gm.server.config.constants.ServerStorageLimits.STORED_VERSION_LENGTH)
    val lastSeenUpdateVersion = varchar("last_seen_update_version", f95gm.server.config.constants.ServerStorageLimits.STORED_VERSION_LENGTH).default("")
    val updatedAt = long("updated_at")

    init {
        index("marks_account_updated_at_idx", false, accountKey, updatedAt)
    }

    override val primaryKey = PrimaryKey(accountKey, threadId)
}
