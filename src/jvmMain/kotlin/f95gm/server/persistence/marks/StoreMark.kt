package f95gm.server.persistence.marks

import f95gm.domain.values.F95AccountKey
import f95gm.server.model.marks.GameMark
import f95gm.server.model.EpochMillis
import f95gm.server.model.ConditionalWriteResult
import f95gm.server.persistence.model.toRecord
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.observability.DatabaseOperation
import f95gm.server.persistence.database.DatabaseEpochMillis
import f95gm.server.persistence.model.requireStorageLength
import f95gm.server.persistence.schema.MarksTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import f95gm.server.config.constants.ServerStorageLimits

internal suspend fun storeMark(database: TransactionBoundary, account: F95AccountKey, mark: GameMark) {
    val record = mark.toRecord()
    database.transactionSuspending(DatabaseOperation.MARKS_STORE) {
        MarksTable.upsert {
            it[MarksTable.accountKey] =
                requireStorageLength(account.value, ServerStorageLimits.ACCOUNT_KEY_LENGTH, "accountKey")
            it[MarksTable.threadId] =
                requireStorageLength(record.threadId, ServerStorageLimits.THREAD_ID_LENGTH, "threadId")
            it[MarksTable.trackingState] = record.trackingState
            it[MarksTable.storedVersion] =
                requireStorageLength(record.storedVersion, ServerStorageLimits.STORED_VERSION_LENGTH, "storedVersion")
            it[MarksTable.lastSeenUpdateVersion] =
                requireStorageLength(
                    record.lastSeenUpdateVersion,
                    ServerStorageLimits.STORED_VERSION_LENGTH,
                    "lastSeenUpdateVersion"
                )
            it[MarksTable.updatedAt] = DatabaseEpochMillis.fromDomain(record.updatedAt).value
        }
    }
}

internal suspend fun storeMarkIfCurrent(
    database: TransactionBoundary,
    account: F95AccountKey,
    mark: GameMark,
    expectedUpdatedAt: EpochMillis?
): ConditionalWriteResult<Unit> = database.transactionSuspending(DatabaseOperation.MARKS_STORE_IF_CURRENT) {
    val record = mark.toRecord()
    val current = MarksTable.selectAll()
        .where { (MarksTable.accountKey eq account.value) and (MarksTable.threadId eq record.threadId) }
        .singleOrNull()
        ?.get(MarksTable.updatedAt)
    if (expectedUpdatedAt != null && current != DatabaseEpochMillis.fromDomain(expectedUpdatedAt).value) {
        return@transactionSuspending ConditionalWriteResult.Conflict(
            expectedUpdatedAt, current?.let { DatabaseEpochMillis(it).toDomain() }
        )
    }
    MarksTable.upsert {
        it[MarksTable.accountKey] =
            requireStorageLength(account.value, ServerStorageLimits.ACCOUNT_KEY_LENGTH, "accountKey")
        it[MarksTable.threadId] =
            requireStorageLength(record.threadId, ServerStorageLimits.THREAD_ID_LENGTH, "threadId")
        it[MarksTable.trackingState] = record.trackingState
        it[MarksTable.storedVersion] =
            requireStorageLength(record.storedVersion, ServerStorageLimits.STORED_VERSION_LENGTH, "storedVersion")
        it[MarksTable.lastSeenUpdateVersion] = requireStorageLength(
            record.lastSeenUpdateVersion,
            ServerStorageLimits.STORED_VERSION_LENGTH,
            "lastSeenUpdateVersion"
        )
        it[MarksTable.updatedAt] = DatabaseEpochMillis.fromDomain(record.updatedAt).value
    }
    ConditionalWriteResult.Stored(Unit)
}
