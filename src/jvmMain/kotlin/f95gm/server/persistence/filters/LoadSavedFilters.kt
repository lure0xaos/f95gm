package f95gm.server.persistence.filters

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95OptionName
import f95gm.server.config.filters.excludedPrefixesKind
import f95gm.server.config.filters.excludedTagsKind
import f95gm.server.config.filters.selectedPrefixesKind
import f95gm.server.config.filters.selectedTagsKind
import f95gm.server.model.filters.SavedFilter
import f95gm.server.persistence.database.dbQuery
import f95gm.server.persistence.schema.SavedFilterOptionsTable
import f95gm.server.persistence.schema.SavedFiltersTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

internal fun loadSavedFilters(account: F95AccountKey): List<SavedFilter> = dbQuery {
    SavedFiltersTable.selectAll()
        .where { SavedFiltersTable.accountKey eq account.value }
        .mapNotNull { row ->
            val filterId = row[SavedFiltersTable.id]
            val options = SavedFilterOptionsTable.selectAll()
                .where { SavedFilterOptionsTable.filterId eq filterId }
                .toList()
            parseSavedFilter(
                SavedFilter(
                    id = F95FilterId(filterId),
                    name = F95OptionName(row[SavedFiltersTable.name]),
                    category = row[SavedFiltersTable.category],
                    sort = row[SavedFiltersTable.sort],
                    search = row[SavedFiltersTable.search],
                    creatorSearch = row[SavedFiltersTable.creatorSearch],
                    searchMode = row[SavedFiltersTable.searchMode],
                    selectedTags = optionsFor(options, selectedTagsKind),
                    excludedTags = optionsFor(options, excludedTagsKind),
                    selectedPrefixes = optionsFor(options, selectedPrefixesKind),
                    excludedPrefixes = optionsFor(options, excludedPrefixesKind),
                    tagType = row[SavedFiltersTable.tagType],
                    dateLimit = row[SavedFiltersTable.dateLimit],
                    updatedAt = row[SavedFiltersTable.updatedAt]
                )
            )
        }
        .sortedByDescending { it.updatedAt }
}
