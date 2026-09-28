package f95gm.server.routes.marks

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.StoredVersion
import f95gm.server.model.marks.GameMark

internal fun effectiveStoredVersion(
    existing: GameMark?,
    requested: GameMark,
    acknowledge: Boolean,
    liveVersion: String?
): StoredVersion {
    if (existing != null || acknowledge) return requested.storedVersion
    return liveVersion
        ?.takeIf { it.isNotBlank() && !it.equals(F95Defaults.UNKNOWN_VERSION, ignoreCase = true) }
        ?.let(::StoredVersion)
        ?: requested.storedVersion
}
