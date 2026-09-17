package f95gm.server.persistence.schema

import f95gm.domain.tracking.GameTrackingState
import org.jetbrains.exposed.v1.core.Table
import f95gm.server.config.constants.FilterLimits
import f95gm.server.config.constants.ServerStorageLimits

internal object MarksTable : Table("marks") {
    val accountKey = varchar("account_key", ServerStorageLimits.ACCOUNT_KEY_LENGTH)
    val threadId = varchar("thread_id", ServerStorageLimits.THREAD_ID_LENGTH)
    val trackingState =
        enumerationByName("tracking_state", FilterLimits.ENUMERATION_LENGTH, GameTrackingState::class)
    val storedVersion = varchar("stored_version", ServerStorageLimits.STORED_VERSION_LENGTH)
    val lastSeenUpdateVersion = varchar("last_seen_update_version", ServerStorageLimits.STORED_VERSION_LENGTH).default("")
    val updatedAt = long("updated_at")

    init {
        index("marks_account_updated_at_idx", false, accountKey, updatedAt)
    }

    override val primaryKey = PrimaryKey(accountKey, threadId)
}
