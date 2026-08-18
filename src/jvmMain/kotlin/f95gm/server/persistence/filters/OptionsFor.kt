package f95gm.server.persistence.filters

import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName
import f95gm.server.model.filters.SavedFilterOption
import f95gm.server.persistence.schema.SavedFilterOptionsTable

internal fun optionsFor(rows: List<org.jetbrains.exposed.v1.core.ResultRow>, kind: String): List<SavedFilterOption> =
    rows.asSequence()
        .filter { it[SavedFilterOptionsTable.kind] == kind }
        .sortedBy { it[SavedFilterOptionsTable.position] }
        .map {
            SavedFilterOption(
                F95OptionId(it[SavedFilterOptionsTable.optionId]),
                F95OptionName(it[SavedFilterOptionsTable.name])
            )
        }
        .toList()
