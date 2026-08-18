package f95gm.server.persistence.filters.backup

import f95gm.server.model.filters.QuickLinksBackup
import f95gm.server.model.filters.SavedFilter
import f95gm.server.state.runtime.upstreamJson

internal fun savedFiltersBackupJson(filters: List<SavedFilter>): String = upstreamJson.encodeToString(
    QuickLinksBackup(quickLinks = filters.map { it.toSavedFilterRequest() })
)
