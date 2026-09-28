package f95gm.server.persistence.filters

import f95gm.server.persistence.model.SavedFilterOptionRecord
import f95gm.server.persistence.schema.SavedFilterOptionsTable

internal fun optionsFor(rows: List<org.jetbrains.exposed.v1.core.ResultRow>, kind: String): List<SavedFilterOptionRecord> =
    rows.asSequence()
        .filter { it[SavedFilterOptionsTable.kind] == kind }
        .sortedBy { it[SavedFilterOptionsTable.position] }
        .map {
            SavedFilterOptionRecord(it[SavedFilterOptionsTable.optionId], it[SavedFilterOptionsTable.name])
        }
        .toList()
