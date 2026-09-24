package f95gm.server.persistence.marks

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95ThreadId
import f95gm.domain.values.StoredVersion
import f95gm.server.model.EpochMillis
import f95gm.server.model.marks.GameMark
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.schema.MarksTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

internal suspend fun loadMarks(database: DatabaseExecutor, account: F95AccountKey): List<GameMark> =
    database.transactionSuspending {
    MarksTable.selectAll()
        .where { MarksTable.accountKey eq account.value }
        .mapNotNull { row ->
            parseMark(
                row[MarksTable.threadId],
                GameMark(
                    threadId = F95ThreadId(row[MarksTable.threadId]),
                    trackingState = row[MarksTable.trackingState],
                    storedVersion = StoredVersion(row[MarksTable.storedVersion]),
                    lastSeenUpdateVersion = StoredVersion(row[MarksTable.lastSeenUpdateVersion]),
                    updatedAt = EpochMillis(row[MarksTable.updatedAt])
                )
            )
        }
        .sortedByDescending { it.updatedAt }
}
