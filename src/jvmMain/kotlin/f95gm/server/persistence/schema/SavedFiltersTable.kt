package f95gm.server.persistence.schema

import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.server.config.constants.FilterLimits
import f95gm.server.config.constants.ServerStorageLimits
import f95gm.server.config.CatalogLimits
import org.jetbrains.exposed.v1.core.Table

internal object SavedFiltersTable : Table("saved_filters") {
    val id = varchar("id", FilterLimits.FILTER_ID_LENGTH)
    val accountKey = varchar("account_key", ServerStorageLimits.ACCOUNT_KEY_LENGTH)
    val name = varchar("name", FilterLimits.FILTER_NAME_LENGTH)
    val category = enumerationByName("category", FilterLimits.ENUMERATION_LENGTH, F95Category::class)
    val sort = enumerationByName("sort", FilterLimits.ENUMERATION_LENGTH, F95Sort::class)
    val search = varchar("search", CatalogLimits.MAX_TEXT_LENGTH)
    val creatorSearch = varchar("creator_search", CatalogLimits.MAX_TEXT_LENGTH)
    val searchMode = enumerationByName("search_mode", FilterLimits.SHORT_ENUMERATION_LENGTH, F95SearchMode::class)
    val tagType = enumerationByName("tag_type", FilterLimits.SHORT_ENUMERATION_LENGTH, F95TagType::class)
    val dateLimit = integer("date_limit")
    val updatedAt = long("updated_at")

    init {
        index("saved_filters_account_updated_at_idx", false, accountKey, updatedAt)
    }

    override val primaryKey = PrimaryKey(id)

    init {
        index("saved_filters_account_name_unique", true, accountKey, name)
    }
}
