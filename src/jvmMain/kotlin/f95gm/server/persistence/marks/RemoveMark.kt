package f95gm.server.persistence.marks

import f95gm.domain.values.F95AccountKey
import f95gm.server.persistence.database.dbQuery
import f95gm.server.persistence.schema.MarksTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere

internal fun removeMark(account: F95AccountKey, threadId: String) {
    dbQuery {
        MarksTable.deleteWhere { (MarksTable.accountKey eq account.value) and (MarksTable.threadId eq threadId) }
    }
}
