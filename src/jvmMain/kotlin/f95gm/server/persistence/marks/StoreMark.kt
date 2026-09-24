package f95gm.server.persistence.marks

import f95gm.domain.values.F95AccountKey
import f95gm.server.config.constants.ServerConstants
import f95gm.server.model.marks.GameMark
import f95gm.server.persistence.ConditionalWriteResult
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.requireStorageLength
import f95gm.server.persistence.schema.MarksTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

internal suspend fun storeMark(database: DatabaseExecutor, account: F95AccountKey, mark: GameMark) {
    database.transactionSuspending {
        MarksTable.upsert {
            it[MarksTable.accountKey] =
                requireStorageLength(account.value, ServerConstants.ACCOUNT_KEY_LENGTH, "accountKey")
            it[MarksTable.threadId] =
                requireStorageLength(mark.threadId.value, ServerConstants.THREAD_ID_LENGTH, "threadId")
            it[MarksTable.trackingState] = mark.trackingState
            it[MarksTable.storedVersion] =
                requireStorageLength(mark.storedVersion.value, ServerConstants.STORED_VERSION_LENGTH, "storedVersion")
            it[MarksTable.lastSeenUpdateVersion] =
                requireStorageLength(
                    mark.lastSeenUpdateVersion.value,
                    ServerConstants.STORED_VERSION_LENGTH,
                    "lastSeenUpdateVersion"
                )
            it[MarksTable.updatedAt] = mark.updatedAt.value
        }
    }
}

internal suspend fun storeMarkIfCurrent(
    database: DatabaseExecutor,
    account: F95AccountKey,
    mark: GameMark,
    expectedUpdatedAt: Long?
): ConditionalWriteResult<Unit> = database.transactionSuspending {
    val current = MarksTable.selectAll()
        .where { (MarksTable.accountKey eq account.value) and (MarksTable.threadId eq mark.threadId.value) }
        .singleOrNull()
        ?.get(MarksTable.updatedAt)
    if (expectedUpdatedAt != null && current != expectedUpdatedAt) return@transactionSuspending ConditionalWriteResult.Conflict
    MarksTable.upsert {
        it[MarksTable.accountKey] =
            requireStorageLength(account.value, ServerConstants.ACCOUNT_KEY_LENGTH, "accountKey")
        it[MarksTable.threadId] =
            requireStorageLength(mark.threadId.value, ServerConstants.THREAD_ID_LENGTH, "threadId")
        it[MarksTable.trackingState] = mark.trackingState
        it[MarksTable.storedVersion] =
            requireStorageLength(mark.storedVersion.value, ServerConstants.STORED_VERSION_LENGTH, "storedVersion")
        it[MarksTable.lastSeenUpdateVersion] = requireStorageLength(
            mark.lastSeenUpdateVersion.value,
            ServerConstants.STORED_VERSION_LENGTH,
            "lastSeenUpdateVersion"
        )
        it[MarksTable.updatedAt] = mark.updatedAt.value
    }
    ConditionalWriteResult.Stored(Unit)
}
