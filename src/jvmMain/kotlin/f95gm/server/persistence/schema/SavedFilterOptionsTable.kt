package f95gm.server.persistence.schema

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

internal object SavedFilterOptionsTable : Table("saved_filter_options") {
    val filterId = varchar("filter_id", f95gm.server.config.constants.FilterLimits.FILTER_ID_LENGTH)
        .references(SavedFiltersTable.id, onDelete = ReferenceOption.CASCADE)
    val kind = varchar("kind", f95gm.server.config.constants.FilterLimits.ENUMERATION_LENGTH)
    val optionId = varchar("option_id", f95gm.server.config.constants.FilterLimits.OPTION_ID_LENGTH)
    val name = varchar("name", f95gm.server.config.constants.FilterLimits.OPTION_NAME_LENGTH)
    val position = integer("position")

    override val primaryKey = PrimaryKey(filterId, kind, optionId)

    init {
        index("saved_filter_options_position_idx", false, filterId, kind, position)
    }
}
