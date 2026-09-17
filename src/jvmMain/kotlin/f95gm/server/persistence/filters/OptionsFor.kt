package f95gm.server.persistence.filters

import f95gm.server.persistence.model.SavedFilterOptionRecord
import f95gm.server.persistence.schema.SavedFilterOptionsTable
import f95gm.server.config.filters.SavedFilterOptionKind

internal fun optionsFor(rows: List<org.jetbrains.exposed.v1.core.ResultRow>, kind: SavedFilterOptionKind): List<SavedFilterOptionRecord> =
    rows.asSequence()
        .filter { it[SavedFilterOptionsTable.kind] == kind.storageKey }
        .sortedBy { it[SavedFilterOptionsTable.position] }
        .map {
            SavedFilterOptionRecord(it[SavedFilterOptionsTable.optionId], it[SavedFilterOptionsTable.name])
        }
        .toList()
