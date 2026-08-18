package f95gm.server.persistence.marks

import f95gm.domain.values.F95AccountKey
import f95gm.server.config.constants.ServerConstants
import f95gm.server.model.marks.GameMark
import f95gm.server.persistence.database.dbQuery
import f95gm.server.persistence.schema.MarksTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert

internal fun storeMark(account: F95AccountKey, mark: GameMark) {
    dbQuery {
        MarksTable.deleteWhere { (MarksTable.accountKey eq account.value) and (MarksTable.threadId eq mark.threadId.value) }
        MarksTable.insert {
            it[MarksTable.accountKey] = account.value.take(ServerConstants.ACCOUNT_KEY_LENGTH)
            it[MarksTable.threadId] = mark.threadId.value
            it[MarksTable.trackingState] = mark.trackingState
            it[MarksTable.storedVersion] = mark.storedVersion.value.take(ServerConstants.STORED_VERSION_LENGTH)
            it[MarksTable.lastSeenUpdateVersion] =
                mark.lastSeenUpdateVersion.value.take(ServerConstants.STORED_VERSION_LENGTH)
            it[MarksTable.updatedAt] = mark.updatedAt
        }
    }
}
