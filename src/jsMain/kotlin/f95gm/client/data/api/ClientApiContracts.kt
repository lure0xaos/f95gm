package f95gm.client.data.api

import f95gm.client.data.catalog.CatalogResult
import f95gm.client.model.catalog.CatalogOptions
import f95gm.client.model.detail.DetailState
import f95gm.client.model.filters.ImportedFiltersDocument
import f95gm.client.model.filters.SavedFilter
import f95gm.client.model.marks.MarkedGame
import f95gm.domain.api.*
import f95gm.domain.filters.F95Category
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId

internal interface SessionApi {
    suspend fun loginTyped(body: LoginRequestContract): ApiResponseEnvelope<OkResponseContract>
    suspend fun logoutTyped(): ApiResponseEnvelope<OkResponseContract>
    suspend fun sessionTyped(): ApiResponseEnvelope<OkResponseContract>
}
internal interface CatalogApi {
    suspend fun catalog(path: ApiPath): CatalogResult
    suspend fun catalogOptions(category: F95Category): CatalogOptions
}
internal interface DetailApi { suspend fun detail(threadId: F95ThreadId): DetailState }
internal interface MarksApi {
    suspend fun marks(): List<MarkedGame>
    suspend fun saveMark(threadId: F95ThreadId, body: F95MarkRequest): MarkedGame
    suspend fun deleteMark(threadId: F95ThreadId)
}
internal interface FiltersApi {
    suspend fun filters(): List<SavedFilter>
    suspend fun saveFilter(body: SavedFilterRequest): List<SavedFilter>
    suspend fun exportFilters(): String
    suspend fun importFilters(body: ImportedFiltersDocument)
    suspend fun deleteFilter(id: F95FilterId)
}
internal interface HealthApi { suspend fun health(path: ApiPath): ApiResponseEnvelope<String> }
internal interface ClientApi : SessionApi, CatalogApi, DetailApi, MarksApi, FiltersApi, HealthApi
