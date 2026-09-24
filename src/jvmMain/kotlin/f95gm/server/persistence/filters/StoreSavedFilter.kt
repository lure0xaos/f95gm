package f95gm.server.persistence.filters

import f95gm.domain.values.F95AccountKey
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.filters.excludedPrefixesKind
import f95gm.server.config.filters.excludedTagsKind
import f95gm.server.config.filters.selectedPrefixesKind
import f95gm.server.config.filters.selectedTagsKind
import f95gm.server.model.filters.SavedFilter
import f95gm.server.persistence.ConditionalWriteResult
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.requireStorageLength
import f95gm.server.persistence.schema.SavedFilterOptionsTable
import f95gm.server.persistence.schema.SavedFiltersTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

internal suspend fun storeSavedFilterIfCurrent(
    database: DatabaseExecutor,
    account: F95AccountKey,
    filter: SavedFilter,
    expectedUpdatedAt: Long?
): ConditionalWriteResult<SavedFilter> = database.transactionSuspending {
    val current = SavedFiltersTable.selectAll()
        .where { SavedFiltersTable.id eq filter.id.value }
        .singleOrNull()
        ?.get(SavedFiltersTable.updatedAt)
    if (expectedUpdatedAt != null && current != expectedUpdatedAt) return@transactionSuspending ConditionalWriteResult.Conflict
    ConditionalWriteResult.Stored(storeSavedFilterInTransaction(account, filter))
}

internal suspend fun storeSavedFilter(
    database: DatabaseExecutor,
    account: F95AccountKey,
    filter: SavedFilter
): SavedFilter = database.transactionSuspending {
    storeSavedFilterInTransaction(account, filter)
}

private fun JdbcTransaction.storeSavedFilterInTransaction(account: F95AccountKey, filter: SavedFilter): SavedFilter {
    val storedFilter = filter.copy(id = uniqueFilterId(account, filter))
    requireStorageLength(account.value, ServerConstants.ACCOUNT_KEY_LENGTH, "accountKey")
    requireStorageLength(storedFilter.name.value, ServerConstants.FILTER_NAME_LENGTH, "filterName")
    SavedFiltersTable.selectAll()
        .where { SavedFiltersTable.accountKey eq account.value.take(ServerConstants.ACCOUNT_KEY_LENGTH) }
        .filter { it[SavedFiltersTable.name].equals(storedFilter.name.value, ignoreCase = true) }
        .forEach { row -> deleteSavedFilterRows(row[SavedFiltersTable.id]) }
    deleteSavedFilterRows(storedFilter.id.value)
    SavedFiltersTable.insert {
        it[SavedFiltersTable.id] = storedFilter.id.value
        it[SavedFiltersTable.accountKey] = account.value.take(ServerConstants.ACCOUNT_KEY_LENGTH)
        it[SavedFiltersTable.name] = storedFilter.name.value.take(ServerConstants.FILTER_NAME_LENGTH)
        it[SavedFiltersTable.category] = storedFilter.category
        it[SavedFiltersTable.sort] = storedFilter.sort
        it[SavedFiltersTable.search] = storedFilter.search.take(ServerConstants.MAX_TEXT_LENGTH)
        it[SavedFiltersTable.creatorSearch] = storedFilter.creatorSearch.take(ServerConstants.MAX_TEXT_LENGTH)
        it[SavedFiltersTable.searchMode] = storedFilter.searchMode
        it[SavedFiltersTable.tagType] = storedFilter.tagType
        it[SavedFiltersTable.dateLimit] = storedFilter.dateLimit
        it[SavedFiltersTable.updatedAt] = storedFilter.updatedAt.value
    }
    listOf(
        selectedTagsKind to storedFilter.selectedTags,
        excludedTagsKind to storedFilter.excludedTags,
        selectedPrefixesKind to storedFilter.selectedPrefixes,
        excludedPrefixesKind to storedFilter.excludedPrefixes
    ).forEach { (kind, options) ->
        options.forEachIndexed { position, option ->
            SavedFilterOptionsTable.insert {
                it[SavedFilterOptionsTable.filterId] = storedFilter.id.value
                it[SavedFilterOptionsTable.kind] = kind
                it[SavedFilterOptionsTable.optionId] = option.id.value
                it[SavedFilterOptionsTable.name] = option.name.value
                it[SavedFilterOptionsTable.position] = position
            }
        }
    }
    return storedFilter
}
