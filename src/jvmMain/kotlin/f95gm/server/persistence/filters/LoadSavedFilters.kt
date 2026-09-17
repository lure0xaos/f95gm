package f95gm.server.persistence.filters

import f95gm.domain.values.F95AccountKey
import f95gm.server.config.filters.SavedFilterOptionKind
import f95gm.server.model.filters.SavedFilter
import f95gm.server.persistence.model.SavedFilterRecord
import f95gm.server.persistence.model.toDomain
import f95gm.server.persistence.database.TransactionBoundary
import f95gm.server.observability.DatabaseOperation
import f95gm.server.persistence.database.DatabaseEpochMillis
import f95gm.server.persistence.schema.SavedFilterOptionsTable
import f95gm.server.persistence.schema.SavedFiltersTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import f95gm.server.model.ParseResult

internal suspend fun loadSavedFilters(database: TransactionBoundary, account: F95AccountKey): List<SavedFilter> =
    database.transactionSuspending(DatabaseOperation.FILTERS_LOAD) {
    SavedFiltersTable.selectAll()
        .where { SavedFiltersTable.accountKey eq account.value }
        .mapNotNull { row ->
            val filterId = row[SavedFiltersTable.id]
            val options = SavedFilterOptionsTable.selectAll()
                .where { SavedFilterOptionsTable.filterId eq filterId }
                .toList()
            (parseSavedFilterResult(
                SavedFilterRecord(
                    id = filterId,
                    name = row[SavedFiltersTable.name],
                    category = row[SavedFiltersTable.category],
                    sort = row[SavedFiltersTable.sort],
                    search = row[SavedFiltersTable.search],
                    creatorSearch = row[SavedFiltersTable.creatorSearch],
                    searchMode = row[SavedFiltersTable.searchMode],
                    selectedTags = optionsFor(options, SavedFilterOptionKind.SELECTED_TAGS),
                    excludedTags = optionsFor(options, SavedFilterOptionKind.EXCLUDED_TAGS),
                    selectedPrefixes = optionsFor(options, SavedFilterOptionKind.SELECTED_PREFIXES),
                    excludedPrefixes = optionsFor(options, SavedFilterOptionKind.EXCLUDED_PREFIXES),
                    tagType = row[SavedFiltersTable.tagType],
                    dateLimit = row[SavedFiltersTable.dateLimit],
                    updatedAt = DatabaseEpochMillis(row[SavedFiltersTable.updatedAt]).toDomain()
                ).toDomain()
            ) as? ParseResult.Success)?.value
        }
        .sortedByDescending { it.updatedAt }
}
