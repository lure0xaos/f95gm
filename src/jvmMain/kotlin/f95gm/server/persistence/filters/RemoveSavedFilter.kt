package f95gm.server.persistence.filters

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95FilterId
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.schema.SavedFiltersTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

internal suspend fun removeSavedFilter(database: DatabaseExecutor, account: F95AccountKey, filterId: F95FilterId) {
    database.transactionSuspending {
        SavedFiltersTable.selectAll()
            .where { (SavedFiltersTable.id eq filterId.value) and (SavedFiltersTable.accountKey eq account.value) }
            .forEach { deleteSavedFilterRows(it[SavedFiltersTable.id]) }
    }
}
