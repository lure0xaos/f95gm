package f95gm.server.persistence.marks

import f95gm.domain.values.F95AccountKey
import f95gm.server.model.marks.GameMark
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.observability.DatabaseOperation
import f95gm.server.persistence.database.DatabaseEpochMillis
import f95gm.server.persistence.schema.MarksTable
import f95gm.server.persistence.model.toDomain
import f95gm.server.persistence.model.MarkRecord
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import f95gm.server.model.ParseResult

internal suspend fun loadMarks(database: TransactionBoundary, account: F95AccountKey): List<GameMark> =
    database.transactionSuspending(DatabaseOperation.MARKS_LOAD) {
    MarksTable.selectAll()
        .where { MarksTable.accountKey eq account.value }
        .mapNotNull { row ->
            (parseMarkResult(
                row[MarksTable.threadId],
                MarkRecord(
                    threadId = row[MarksTable.threadId], trackingState = row[MarksTable.trackingState],
                    storedVersion = row[MarksTable.storedVersion],
                    lastSeenUpdateVersion = row[MarksTable.lastSeenUpdateVersion],
                    updatedAt = DatabaseEpochMillis(row[MarksTable.updatedAt]).toDomain()
                ).toDomain()
            ) as? ParseResult.Success)?.value
        }
        .sortedByDescending { it.updatedAt }
}
