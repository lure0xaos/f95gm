package f95gm.server.persistence.marks

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.StoredVersion
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.persistence.MarkRepository
import f95gm.server.persistence.filters.version.isNewerVersion

internal suspend fun resetTrackingStateForNewVersion(
    account: F95AccountKey,
    marks: List<GameMark>,
    live: Map<String, LiveMarkInfo>,
    repository: MarkRepository
): List<GameMark> {
    val updated = marks.map { mark ->
        val latestVersion = live[mark.threadId.value]?.version?.value ?: F95Defaults.UNKNOWN_VERSION
        val newVersionDetected = isNewerVersion(latestVersion, mark.storedVersion.value) &&
                !latestVersion.equals(mark.lastSeenUpdateVersion.value, ignoreCase = true)
        if (!newVersionDetected) mark else mark.copy(
            trackingState = GameTrackingState.WATCHING,
            lastSeenUpdateVersion = StoredVersion(latestVersion)
        )
    }
    updated.zip(marks).forEach { (newMark, oldMark) ->
        if (newMark != oldMark) repository.store(account, newMark)
    }
    return updated
}
