package f95gm.server.application.port

import f95gm.domain.api.SavedFilterRequest
import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95FilterId
import f95gm.server.kernel.ServerClock
import f95gm.server.model.ConditionalWriteResult
import f95gm.server.model.EpochMillis
import f95gm.server.model.filters.SavedFilter

internal data class ImportResult(val imported: Int)

internal interface SavedFilterApplicationPort {
    suspend fun load(account: F95AccountKey): List<SavedFilter>
    fun fromRequest(request: SavedFilterRequest, clock: ServerClock): SavedFilter?
    suspend fun storeAtomically(account: F95AccountKey, filter: SavedFilter, expectedUpdatedAt: EpochMillis?): ConditionalWriteResult<SavedFilter>
    suspend fun importAtomically(account: F95AccountKey, filter: SavedFilter): SavedFilter
    suspend fun importAtomically(account: F95AccountKey, filters: List<SavedFilter>): ImportResult
    suspend fun removeAtomically(account: F95AccountKey, filterId: F95FilterId)
}
