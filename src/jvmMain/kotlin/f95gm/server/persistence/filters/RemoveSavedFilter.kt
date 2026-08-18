package f95gm.server.persistence.filters

import f95gm.domain.values.F95AccountKey
import f95gm.server.persistence.database.dbQuery
import f95gm.server.persistence.schema.SavedFiltersTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

internal fun removeSavedFilter(account: F95AccountKey, filterId: String) {
    dbQuery {
        SavedFiltersTable.selectAll()
            .where { (SavedFiltersTable.id eq filterId) and (SavedFiltersTable.accountKey eq account.value) }
            .forEach { deleteSavedFilterRows(it[SavedFiltersTable.id]) }
    }
}
