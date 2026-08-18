package f95gm.server.persistence.filters

import f95gm.server.persistence.schema.SavedFilterOptionsTable
import f95gm.server.persistence.schema.SavedFiltersTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere

internal fun deleteSavedFilterRows(filterId: String) {
    SavedFilterOptionsTable.deleteWhere { SavedFilterOptionsTable.filterId eq filterId }
    SavedFiltersTable.deleteWhere { SavedFiltersTable.id eq filterId }
}
