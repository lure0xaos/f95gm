package f95gm.client.data.api

import f95gm.client.data.catalog.parseCatalog
import f95gm.client.data.catalog.parseCatalogOptions
import f95gm.client.data.detail.parseDetail
import f95gm.client.data.filters.parseSavedFilters
import f95gm.client.data.marks.parseMarks
import f95gm.domain.api.OkResponseContract
import f95gm.client.state.runtime.clientJson

/** Typed decoding and fallback policy for every generated API operation. */
internal object ClientEndpointCatalog {
    val login = endpoint(GeneratedF95ApiOperations.post_api_login, ::decodeOkResponse)
    val logout = endpoint(GeneratedF95ApiOperations.post_api_logout, ::decodeOkResponse)
    val session = endpoint(GeneratedF95ApiOperations.get_api_session, ::decodeOkResponse, ApiFallbackMessage.CATALOG_REQUEST_FAILED)
    val catalog = endpoint(GeneratedF95ApiOperations.get_api_catalog, ::parseCatalog, ApiFallbackMessage.CATALOG_REQUEST_FAILED)
    val catalogOptions = endpoint(
        GeneratedF95ApiOperations.get_api_catalog_options,
        ::parseCatalogOptions,
        ApiFallbackMessage.CATALOG_REQUEST_FAILED
    )
    val marks = endpoint(GeneratedF95ApiOperations.get_api_marks, ::parseMarks, ApiFallbackMessage.CATALOG_REQUEST_FAILED)
    val filters = endpoint(GeneratedF95ApiOperations.get_api_filters, ::parseSavedFilters, ApiFallbackMessage.CATALOG_REQUEST_FAILED)
    val detail = endpoint(GeneratedF95ApiOperations.get_api_item__threadId_, ::parseDetail, ApiFallbackMessage.CATALOG_REQUEST_FAILED)
    val saveFilter = endpoint(GeneratedF95ApiOperations.post_api_filters, ::parseSavedFilters)
    val exportFilters = endpoint(GeneratedF95ApiOperations.get_api_filters_export, { it }, ApiFallbackMessage.CATALOG_REQUEST_FAILED)
    val importFilters = endpoint(GeneratedF95ApiOperations.post_api_filters_import, { Unit })
    val deleteFilter = endpoint(GeneratedF95ApiOperations.delete_api_filters__filterId_, { Unit })
    val saveMark = endpoint(GeneratedF95ApiOperations.put_api_marks__threadId_, ::parseMarks)
    val deleteMark = endpoint(GeneratedF95ApiOperations.delete_api_marks__threadId_, { Unit })
    val health = endpoint(GeneratedF95ApiOperations.get_api_health, { it })

    private fun decodeOkResponse(value: String): OkResponseContract = clientJson.decodeFromString(value)

    private inline fun <reified T> endpoint(
        operation: GeneratedF95ApiOperation,
        noinline decode: (String) -> T,
        fallback: ApiFallbackMessage = ApiFallbackMessage.REQUEST_FAILED
    ): ApiEndpointDescriptor<T> = ApiEndpointDescriptor(operation, decode, fallback)
}
