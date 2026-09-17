package f95gm.server.persistence.filters

import f95gm.domain.values.F95AccountKey
import f95gm.server.config.filters.SavedFilterOptionKind
import f95gm.server.model.filters.SavedFilter
import f95gm.server.model.EpochMillis
import f95gm.server.model.ConditionalWriteResult
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.observability.DatabaseOperation
import f95gm.server.persistence.database.DatabaseEpochMillis
import f95gm.server.persistence.model.requireStorageLength
import f95gm.server.persistence.model.toRecord
import f95gm.server.persistence.schema.SavedFilterOptionsTable
import f95gm.server.persistence.schema.SavedFiltersTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

internal suspend fun storeSavedFilterIfCurrent(
    database: TransactionBoundary,
    account: F95AccountKey,
    filter: SavedFilter,
    expectedUpdatedAt: EpochMillis?
): ConditionalWriteResult<SavedFilter> = database.transactionSuspending(DatabaseOperation.FILTERS_STORE_IF_CURRENT) {
    val current = SavedFiltersTable.selectAll()
        .where { SavedFiltersTable.id eq filter.id.value }
        .singleOrNull()
        ?.get(SavedFiltersTable.updatedAt)
    if (expectedUpdatedAt != null && current != DatabaseEpochMillis.fromDomain(expectedUpdatedAt).value) {
        return@transactionSuspending ConditionalWriteResult.Conflict(
            expectedUpdatedAt, current?.let { DatabaseEpochMillis(it).toDomain() }
        )
    }
    ConditionalWriteResult.Stored(storeSavedFilterInTransaction(account, filter))
}

internal suspend fun storeSavedFilter(
    database: TransactionBoundary,
    account: F95AccountKey,
    filter: SavedFilter
): SavedFilter = database.transactionSuspending(DatabaseOperation.FILTERS_STORE) {
    storeSavedFilterInTransaction(account, filter)
}

internal suspend fun storeSavedFilters(
    database: TransactionBoundary,
    account: F95AccountKey,
    filters: List<SavedFilter>
): List<SavedFilter> = database.transactionSuspending(DatabaseOperation.FILTERS_STORE_ALL) {
    filters.map { storeSavedFilterInTransaction(account, it) }
}

private fun JdbcTransaction.storeSavedFilterInTransaction(account: F95AccountKey, filter: SavedFilter): SavedFilter {
    val storedFilter = filter.copy(id = uniqueFilterId(account, filter))
    val record = storedFilter.toRecord()
    SavedFilterStoragePolicy.validate(account, storedFilter)
    SavedFiltersTable.selectAll()
        .where { SavedFiltersTable.accountKey eq SavedFilterStoragePolicy.accountKey(account) }
        .filter { it[SavedFiltersTable.name].equals(storedFilter.name.value, ignoreCase = true) }
        .forEach { row -> deleteSavedFilterRows(row[SavedFiltersTable.id]) }
    deleteSavedFilterRows(storedFilter.id.value)
    SavedFiltersTable.insert {
        it[SavedFiltersTable.id] = record.id
        it[SavedFiltersTable.accountKey] = SavedFilterStoragePolicy.accountKey(account)
        it[SavedFiltersTable.name] = SavedFilterStoragePolicy.name(record.name)
        it[SavedFiltersTable.category] = record.category
        it[SavedFiltersTable.sort] = record.sort
        it[SavedFiltersTable.search] = SavedFilterStoragePolicy.search(record.search)
        it[SavedFiltersTable.creatorSearch] = SavedFilterStoragePolicy.search(record.creatorSearch)
        it[SavedFiltersTable.searchMode] = record.searchMode
        it[SavedFiltersTable.tagType] = record.tagType
        it[SavedFiltersTable.dateLimit] = record.dateLimit
        it[SavedFiltersTable.updatedAt] = DatabaseEpochMillis.fromDomain(record.updatedAt).value
    }
    listOf(
        SavedFilterOptionKind.SELECTED_TAGS to record.selectedTags,
        SavedFilterOptionKind.EXCLUDED_TAGS to record.excludedTags,
        SavedFilterOptionKind.SELECTED_PREFIXES to record.selectedPrefixes,
        SavedFilterOptionKind.EXCLUDED_PREFIXES to record.excludedPrefixes
    ).forEach { (kind, options) ->
        options.forEachIndexed { position, option ->
            SavedFilterOptionsTable.insert {
                it[SavedFilterOptionsTable.filterId] = record.id
                it[SavedFilterOptionsTable.kind] = kind.storageKey
                it[SavedFilterOptionsTable.optionId] = option.id
                it[SavedFilterOptionsTable.name] = option.name
                it[SavedFilterOptionsTable.position] = position
            }
        }
    }
    return storedFilter
}
