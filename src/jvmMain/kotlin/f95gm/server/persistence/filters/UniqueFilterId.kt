package f95gm.server.persistence.filters

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95FilterId
import f95gm.server.config.constants.ServerConstants
import f95gm.server.model.filters.SavedFilter
import f95gm.server.persistence.filters.alias.filterAlias
import f95gm.server.persistence.schema.SavedFiltersTable
import org.jetbrains.exposed.v1.jdbc.selectAll

internal fun uniqueFilterId(account: F95AccountKey, filter: SavedFilter): F95FilterId {
    val storedAccount = account.value.take(ServerConstants.ACCOUNT_KEY_LENGTH)
    val existing = SavedFiltersTable.selectAll().map { row ->
        Triple(row[SavedFiltersTable.id], row[SavedFiltersTable.accountKey], row[SavedFiltersTable.name])
    }
    val base = filterAlias(filter.name.value)
    var candidate = base
    var suffix = 2
    while (existing.any { (id, accountKey, name) ->
            id == candidate && !(accountKey == storedAccount && name.equals(filter.name.value, ignoreCase = true))
        }) {
        val suffixText = "-$suffix"
        candidate = base.take((ServerConstants.FILTER_ID_LENGTH - suffixText.length).coerceAtLeast(1)) + suffixText
        suffix++
    }
    return F95FilterId(candidate)
}
