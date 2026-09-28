package f95gm.domain.api

/** The shared API vocabulary is declared directly in this package. */
enum class ApiMethod(val wireValue: String) {
    GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE");
}

enum class AuthRequirement { NONE, SESSION }
enum class MutationKind { READ, WRITE }
enum class RateLimitScope { NONE, IP, IP_AND_ACCOUNT }
enum class ResponseBody { JSON, TEXT, BINARY, EMPTY }
enum class ParameterType { STRING, INTEGER, STRING_ARRAY }
enum class ParameterLocation { PATH, QUERY }

enum class ApiTag(val wireName: String) {
    SESSION("Session"),
    CATALOG("Catalog"),
    MARKS("Marks"),
    SAVED_FILTERS("Saved filters"),
    ITEMS("Items"),
    SERVER("Server")
}

/** Media types allowed in the generated HTTP contract. */
enum class ApiMediaType(val wireName: String) {
    JSON("application/json"),
    TEXT("text/plain"),
    HTML("text/html"),
    BINARY("application/octet-stream"),
    IMAGE_WILDCARD("image/*")
}

data class ParameterDescriptor(val name: String, val type: ParameterType, val location: ParameterLocation) {
    val path: Boolean get() = location == ParameterLocation.PATH
}

data class ApiOperationDocumentation(val summary: String, val description: String)

enum class ApiSchema(val wireName: String) {
    JSON_OBJECT("JsonObject"), API_ERROR("ApiError"), LOGIN_REQUEST("LoginRequest"), OK_RESPONSE("OkResponse"),
    CATALOG_OPTIONS("CatalogOptions"), MARKS("Marks"), SAVED_FILTERS("SavedFilters"),
    SAVED_FILTER_REQUEST("SavedFilterRequest"), QUICK_LINKS_BACKUP("QuickLinksBackup"),
    IMPORTED_QUICK_LINKS("ImportedQuickLinks"), MARK_REQUEST("MarkRequest"),
    THREAD_DETAIL("ThreadDetail"), DETAIL_META("DetailMeta"), DETAIL_SECTION("DetailSection"),
    SAVED_FILTER("SavedFilter"), SAVED_FILTER_OPTION("SavedFilterOption"), MARK_RESPONSE("MarkResponse"),
    REQUEST_METRICS("RequestMetrics"), EMPTY_REQUEST("EmptyRequest"),
    UPSTREAM_CATALOG("UpstreamCatalog"), MEDIA("Media")
}

data class EndpointDescriptor(
    val path: ApiPath,
    val methods: Set<ApiMethod>,
    val auth: AuthRequirement,
    val mutation: MutationKind,
    val rateLimit: RateLimitScope,
    val tag: ApiTag,
    val successStatus: F95ApiStatus = F95ApiStatus.OK,
    val responseBody: ResponseBody = ResponseBody.JSON,
    val responseSchema: ApiSchema = ApiSchema.JSON_OBJECT,
    val requestSchema: ApiSchema? = null,
    val documentation: ApiOperationDocumentation,
    val queryParameters: List<ApiQueryParameter> = emptyList(),
    val pathParameters: List<ApiPathParameter> = emptyList(),
    val errorStatuses: Set<F95ApiStatus> = F95ApiStatus.DEFAULT_ERROR_STATUSES,
    val emptyResponseStatuses: Set<F95ApiStatus> = emptySet(),
    val apiVersion: ApiVersion = ApiVersion("1"),
    val deprecation: ApiDeprecationMetadata? = null
)
