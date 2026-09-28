package f95gm.server.persistence.marks

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95ThreadId
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.observability.DatabaseOperation
import f95gm.server.persistence.schema.MarksTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere

internal suspend fun removeMark(database: TransactionBoundary, account: F95AccountKey, threadId: F95ThreadId) {
    database.transactionSuspending(DatabaseOperation.MARKS_REMOVE) {
        MarksTable.deleteWhere { (MarksTable.accountKey eq account.value) and (MarksTable.threadId eq threadId.value) }
    }
}
