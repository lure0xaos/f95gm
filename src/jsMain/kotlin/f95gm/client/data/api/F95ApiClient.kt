package f95gm.client.data.api

import f95gm.domain.api.ApiPath
import f95gm.domain.api.ApiResponseEnvelope
import f95gm.domain.api.CatalogOptionsContract
import f95gm.domain.api.LoginRequestContract
import f95gm.domain.api.MarkResponseContract
import f95gm.domain.api.OkResponseContract
import f95gm.domain.api.RequestMetricsContract
import f95gm.domain.api.SavedFilterContract
import f95gm.domain.api.ThreadDetailContract
import f95gm.domain.api.ApiMethod
import f95gm.domain.api.F95ApiPaths
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import f95gm.domain.filters.F95Category
import f95gm.client.data.catalog.CatalogResult
import f95gm.client.data.catalog.parseCatalog
import f95gm.client.data.catalog.parseCatalogOptions
import f95gm.client.data.detail.parseDetail
import f95gm.client.data.filters.parseSavedFilters
import f95gm.client.data.marks.parseMarks
import f95gm.client.model.catalog.CatalogOptions
import f95gm.client.model.detail.DetailState
import f95gm.client.model.filters.SavedFilter
import f95gm.client.model.marks.MarkedGame
import f95gm.client.model.filters.ImportedFiltersDocument
import f95gm.domain.api.F95MarkRequest
import f95gm.domain.api.SavedFilterRequest
import f95gm.messages.UiMessages
import f95gm.client.state.runtime.clientJson
import f95gm.client.model.runtime.HttpStatusCodeValue
import io.ktor.http.HttpMethod
import kotlinx.serialization.encodeToString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonObject

internal suspend fun executeOperation(
    operation: GeneratedF95ApiOperation,
    path: ApiPath = operation.path,
    body: ApiRequestBody? = null,
    executor: ApiRequestExecutor,
    fallbackOverride: ApiStatusFallback? = null,
    retryPolicy: RetryPolicy = RetryPolicy(),
    endpoint: f95gm.client.model.runtime.ApiEndpointId = f95gm.client.model.runtime.ApiEndpointId(operation.id.value)
): ApiResponseEnvelope<String> {
    check(operation.id in GeneratedF95ApiOperations.operationIds) { "Unsupported API operation: ${operation.id}" }
    val method = when (operation.method) {
        ApiMethod.GET -> HttpMethod.Get
        ApiMethod.POST -> HttpMethod.Post
        ApiMethod.PUT -> HttpMethod.Put
        ApiMethod.DELETE -> HttpMethod.Delete
    }
    val fallback = fallbackOverride ?: if (method == HttpMethod.Get) {
        ApiStatusFallback { status -> UiMessages.app_catalogRequestFailed(status.value) }
    } else {
        ApiStatusFallback { status -> UiMessages.app_requestFailed(status.value) }
    }
    return requestJsonEnvelope(
        path = path,
        method = method,
        body = body,
        fallback = fallback,
        endpoint = endpoint,
        transport = executor.transport,
        events = NoopApiClientEvents,
        executor = executor,
        retryPolicy = retryPolicy
    )
}

/** Runtime-owned API facade used by migrated client actions. */
internal class RuntimeApiClient(private val executor: ApiRequestExecutor) : ClientApi {
    private suspend fun <T> execute(
        endpoint: ApiEndpointDescriptor<T>,
        path: ApiPath = endpoint.operation.path,
        body: ApiRequestBody? = null
    ): T = endpoint.execute(executor, path, body).body

    override suspend fun loginTyped(body: LoginRequestContract): ApiResponseEnvelope<OkResponseContract> =
        ClientEndpointCatalog.login.execute(executor, body = ApiRequestBody(clientJson.encodeToString(body)))

    override suspend fun logoutTyped(): ApiResponseEnvelope<OkResponseContract> =
        ClientEndpointCatalog.logout.execute(executor)

    override suspend fun sessionTyped(): ApiResponseEnvelope<OkResponseContract> =
        ClientEndpointCatalog.session.execute(executor)

    override suspend fun catalog(path: ApiPath): CatalogResult =
        execute(ClientEndpointCatalog.catalog, path)
    override suspend fun catalogOptions(category: F95Category): CatalogOptions =
        execute(ClientEndpointCatalog.catalogOptions, F95ApiPaths.catalogOptionsPath(category))
    override suspend fun marks(): List<MarkedGame> = execute(ClientEndpointCatalog.marks)
    override suspend fun filters(): List<SavedFilter> = execute(ClientEndpointCatalog.filters)

    override suspend fun detail(threadId: F95ThreadId): DetailState =
        execute(ClientEndpointCatalog.detail, F95ApiPaths.itemPath(threadId))

    override suspend fun saveFilter(body: SavedFilterRequest): List<SavedFilter> =
        execute(ClientEndpointCatalog.saveFilter, body = ApiRequestBody(clientJson.encodeToString(body)))

    override suspend fun exportFilters(): String = execute(ClientEndpointCatalog.exportFilters)

    override suspend fun importFilters(body: ImportedFiltersDocument) {
        execute(ClientEndpointCatalog.importFilters, body = ApiRequestBody(body.value))
    }

    override suspend fun deleteFilter(id: F95FilterId) {
        execute(ClientEndpointCatalog.deleteFilter, F95ApiPaths.filterPath(id))
    }

    override suspend fun saveMark(threadId: F95ThreadId, body: F95MarkRequest): MarkedGame =
        execute(ClientEndpointCatalog.saveMark, F95ApiPaths.markPath(threadId), ApiRequestBody(clientJson.encodeToString(body)))
            .firstOrNull() ?: throw ApiException(
                path = F95ApiPaths.markPath(threadId),
                message = "Mark response did not contain a marked game",
                causeKind = ApiExceptionCause.PAYLOAD
            )

    override suspend fun deleteMark(threadId: F95ThreadId) {
        execute(ClientEndpointCatalog.deleteMark, F95ApiPaths.markPath(threadId))
    }

    override suspend fun health(path: ApiPath): ApiResponseEnvelope<String> =
        ClientEndpointCatalog.health.execute(executor, path)
}
