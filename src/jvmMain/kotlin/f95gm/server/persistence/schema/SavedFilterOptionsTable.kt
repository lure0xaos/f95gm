package f95gm.server.persistence.schema

import f95gm.server.config.constants.ServerConstants
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

internal object SavedFilterOptionsTable : Table("saved_filter_options") {
    val filterId = varchar("filter_id", ServerConstants.FILTER_ID_LENGTH)
        .references(SavedFiltersTable.id, onDelete = ReferenceOption.CASCADE)
    val kind = varchar("kind", ServerConstants.ENUMERATION_LENGTH)
    val optionId = varchar("option_id", ServerConstants.OPTION_ID_LENGTH)
    val name = varchar("name", ServerConstants.OPTION_NAME_LENGTH)
    val position = integer("position")

    override val primaryKey = PrimaryKey(filterId, kind, optionId)
}
