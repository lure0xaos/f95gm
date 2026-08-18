package f95gm.server.persistence.schema

import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.server.config.constants.ServerConstants
import org.jetbrains.exposed.v1.core.Table

internal object SavedFiltersTable : Table("saved_filters") {
    val id = varchar("id", ServerConstants.FILTER_ID_LENGTH)
    val accountKey = varchar("account_key", ServerConstants.ACCOUNT_KEY_LENGTH)
    val name = varchar("name", ServerConstants.FILTER_NAME_LENGTH)
    val category = enumerationByName("category", ServerConstants.ENUMERATION_LENGTH, F95Category::class)
    val sort = enumerationByName("sort", ServerConstants.ENUMERATION_LENGTH, F95Sort::class)
    val search = varchar("search", ServerConstants.MAX_TEXT_LENGTH)
    val creatorSearch = varchar("creator_search", ServerConstants.MAX_TEXT_LENGTH)
    val searchMode = enumerationByName("search_mode", ServerConstants.SHORT_ENUMERATION_LENGTH, F95SearchMode::class)
    val tagType = enumerationByName("tag_type", ServerConstants.SHORT_ENUMERATION_LENGTH, F95TagType::class)
    val dateLimit = integer("date_limit")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index("saved_filters_account_name_unique", true, accountKey, name)
    }
}
