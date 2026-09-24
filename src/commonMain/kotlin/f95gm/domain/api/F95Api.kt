package f95gm.domain.api

import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId

object F95Api {
    enum class ApiMethod(val wireValue: String) {
        GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE")
    }

    enum class AuthRequirement { NONE, SESSION }
    enum class MutationKind { READ, WRITE }
    enum class RateLimitScope { NONE, IP, IP_AND_ACCOUNT }
    enum class ResponseBody { JSON, TEXT, BINARY, EMPTY }

    data class EndpointDescriptor(
        val path: String,
        val methods: Set<ApiMethod>,
        val auth: AuthRequirement,
        val mutation: MutationKind,
        val rateLimit: RateLimitScope,
        val successStatus: F95ApiStatus = F95ApiStatus.OK,
        val responseBody: ResponseBody = ResponseBody.JSON,
        val responseSchema: String = "JsonObject",
        val requestSchema: String? = null,
        val queryParameters: List<String> = emptyList(),
        val pathParameters: List<String> = emptyList(),
        val errorStatuses: Set<F95ApiStatus> = F95ApiStatus.DEFAULT_ERROR_STATUSES,
        val emptyResponseStatuses: Set<F95ApiStatus> = emptySet()
    )
    const val BASE: String = "/api"
    const val LOGIN: String = "$BASE/login"
    const val LOGOUT: String = "$BASE/logout"
    const val SESSION: String = "$BASE/session"
    const val CATALOG: String = "$BASE/catalog"
    const val CATALOG_OPTIONS: String = "$CATALOG/options"
    const val MARKS: String = "$BASE/marks"
    const val MARKS_CHECK: String = "$MARKS/check"
    const val FILTERS: String = "$BASE/filters"
    const val FILTERS_EXPORT: String = "$FILTERS/export"
    const val FILTERS_IMPORT: String = "$FILTERS/import"
    const val ITEM: String = "$BASE/item"
    const val MEDIA: String = "$BASE/media"
    const val HEALTH: String = "$BASE/health"
    const val LIVE: String = "$BASE/live"
    const val READY: String = "$BASE/ready"
    const val METRICS: String = "$BASE/metrics"
    const val SHUTDOWN: String = "$BASE/shutdown"
    const val FILTER_ID: String = "filterId"
    const val THREAD_ID: String = "threadId"
    const val MARK_BY_THREAD: String = "$MARKS/{$THREAD_ID}"
    const val FILTER_BY_ID: String = "$FILTERS/{$FILTER_ID}"
    const val ITEM_BY_THREAD: String = "$ITEM/{$THREAD_ID}"
    const val TAB_ID: String = "tabId"
    const val THEME: String = "theme"

    const val CATEGORY: String = "category"
    const val PAGE: String = "page"
    const val ROWS: String = "rows"
    const val SORT: String = "sort"
    const val SEARCH: String = "search"
    const val CREATOR: String = "creator"
    const val DATE: String = "date"
    const val TAG_TYPE: String = "tagtype"
    const val TAGS: String = "tags"
    const val EXCLUDED_TAGS: String = "notags"
    const val PREFIXES: String = "prefixes"
    const val EXCLUDED_PREFIXES: String = "noprefixes"

    fun markPath(threadId: F95ThreadId): String = "$MARKS/${threadId.value}"
    fun filterPath(filterId: F95FilterId): String = "$FILTERS/${filterId.value}"
    fun itemPath(threadId: F95ThreadId): String = "$ITEM/${threadId.value}"
    fun catalogOptionsPath(category: String): String = "$CATALOG_OPTIONS?$CATEGORY=$category"

    val endpointDescriptors: List<EndpointDescriptor> = listOf(
        endpoint(
            LOGIN,
            ApiMethod.POST,
            AuthRequirement.NONE,
            MutationKind.WRITE,
            RateLimitScope.IP_AND_ACCOUNT,
            request = "LoginRequest",
            errors = setOf(
                F95ApiStatus.BAD_REQUEST, F95ApiStatus.UNAUTHORIZED, F95ApiStatus.TOO_MANY_REQUESTS,
                F95ApiStatus.INTERNAL_SERVER_ERROR, F95ApiStatus.BAD_GATEWAY, F95ApiStatus.SERVICE_UNAVAILABLE,
                F95ApiStatus.GATEWAY_TIMEOUT
            )
        ),
        endpoint(
            LOGOUT,
            ApiMethod.POST,
            AuthRequirement.SESSION,
            MutationKind.WRITE,
            RateLimitScope.IP_AND_ACCOUNT,
            request = "EmptyRequest"
        ),
        endpoint(
            SESSION,
            ApiMethod.GET,
            AuthRequirement.NONE,
            MutationKind.READ,
            RateLimitScope.IP_AND_ACCOUNT,
            response = "OkResponse",
            errors = setOf(
                F95ApiStatus.UNAUTHORIZED, F95ApiStatus.TOO_MANY_REQUESTS,
                F95ApiStatus.INTERNAL_SERVER_ERROR, F95ApiStatus.BAD_GATEWAY, F95ApiStatus.SERVICE_UNAVAILABLE,
                F95ApiStatus.GATEWAY_TIMEOUT
            )
        ),
        endpoint(
            CATALOG,
            ApiMethod.GET,
            AuthRequirement.SESSION,
            MutationKind.READ,
            RateLimitScope.IP_AND_ACCOUNT,
            responseBody = ResponseBody.TEXT,
            response = "UpstreamCatalog",
            query = listOf(
                CATEGORY,
                PAGE,
                ROWS,
                SORT,
                TAGS,
                EXCLUDED_TAGS,
                PREFIXES,
                EXCLUDED_PREFIXES,
                TAG_TYPE,
                DATE,
                SEARCH,
                CREATOR
            ),
            errors = setOf(
                F95ApiStatus.BAD_REQUEST, F95ApiStatus.UNAUTHORIZED, F95ApiStatus.TOO_MANY_REQUESTS,
                F95ApiStatus.INTERNAL_SERVER_ERROR, F95ApiStatus.BAD_GATEWAY, F95ApiStatus.SERVICE_UNAVAILABLE,
                F95ApiStatus.GATEWAY_TIMEOUT
            )
        ),
        endpoint(
            CATALOG_OPTIONS,
            ApiMethod.GET,
            AuthRequirement.SESSION,
            MutationKind.READ,
            RateLimitScope.IP_AND_ACCOUNT,
            response = "CatalogOptions",
            query = listOf(CATEGORY)
        ),
        endpoint(
            MARKS,
            ApiMethod.GET,
            AuthRequirement.SESSION,
            MutationKind.READ,
            RateLimitScope.IP_AND_ACCOUNT,
            response = "Marks"
        ),
        endpoint(
            MARKS_CHECK,
            ApiMethod.GET,
            AuthRequirement.SESSION,
            MutationKind.READ,
            RateLimitScope.IP_AND_ACCOUNT,
            response = "Marks"
        ),
        endpoint(
            FILTERS,
            ApiMethod.GET,
            AuthRequirement.SESSION,
            MutationKind.READ,
            RateLimitScope.IP_AND_ACCOUNT,
            response = "SavedFilters"
        ),
        endpoint(
            FILTERS,
            ApiMethod.POST,
            AuthRequirement.SESSION,
            MutationKind.WRITE,
            RateLimitScope.IP_AND_ACCOUNT,
            request = "SavedFilterRequest",
            response = "SavedFilters"
        ),
        endpoint(
            FILTERS_EXPORT,
            ApiMethod.GET,
            AuthRequirement.SESSION,
            MutationKind.READ,
            RateLimitScope.IP_AND_ACCOUNT,
            response = "QuickLinksBackup"
        ),
        endpoint(
            FILTERS_IMPORT,
            ApiMethod.POST,
            AuthRequirement.SESSION,
            MutationKind.WRITE,
            RateLimitScope.IP_AND_ACCOUNT,
            request = "QuickLinksBackup",
            response = "ImportedQuickLinks"
        ),
        endpoint(
            FILTER_BY_ID,
            ApiMethod.DELETE,
            AuthRequirement.SESSION,
            MutationKind.WRITE,
            RateLimitScope.IP_AND_ACCOUNT,
            response = "OkResponse",
            pathParams = listOf(FILTER_ID)
        ),
        endpoint(
            MARK_BY_THREAD,
            ApiMethod.PUT,
            AuthRequirement.SESSION,
            MutationKind.WRITE,
            RateLimitScope.IP_AND_ACCOUNT,
            request = "MarkRequest",
            response = "Marks",
            pathParams = listOf(THREAD_ID)
        ),
        endpoint(
            MARK_BY_THREAD,
            ApiMethod.DELETE,
            AuthRequirement.SESSION,
            MutationKind.WRITE,
            RateLimitScope.IP_AND_ACCOUNT,
            response = "OkResponse",
            pathParams = listOf(THREAD_ID)
        ),
        endpoint(
            ITEM_BY_THREAD,
            ApiMethod.GET,
            AuthRequirement.SESSION,
            MutationKind.READ,
            RateLimitScope.IP_AND_ACCOUNT,
            response = "ThreadDetail",
            pathParams = listOf(THREAD_ID)
        ),
        endpoint(
            MEDIA,
            ApiMethod.GET,
            AuthRequirement.SESSION,
            MutationKind.READ,
            RateLimitScope.IP_AND_ACCOUNT,
            responseBody = ResponseBody.BINARY,
            response = "Media",
            query = listOf("url")
        ),
        endpoint(
            HEALTH,
            ApiMethod.GET,
            AuthRequirement.NONE,
            MutationKind.READ,
            RateLimitScope.NONE,
            responseBody = ResponseBody.EMPTY,
            errors = emptySet()
        ),
        endpoint(
            LIVE,
            ApiMethod.GET,
            AuthRequirement.NONE,
            MutationKind.READ,
            RateLimitScope.NONE,
            responseBody = ResponseBody.EMPTY,
            errors = emptySet()
        ),
        endpoint(
            READY,
            ApiMethod.GET,
            AuthRequirement.NONE,
            MutationKind.READ,
            RateLimitScope.NONE,
            responseBody = ResponseBody.EMPTY,
            errors = emptySet(),
            extraStatuses = setOf(F95ApiStatus.SERVICE_UNAVAILABLE),
            emptyResponses = setOf(F95ApiStatus.SERVICE_UNAVAILABLE)
        ),
        endpoint(
            METRICS,
            ApiMethod.GET,
            AuthRequirement.NONE,
            MutationKind.READ,
            RateLimitScope.NONE,
            response = "RequestMetrics",
            errors = emptySet()
        ),
        endpoint(
            SHUTDOWN,
            ApiMethod.POST,
            AuthRequirement.NONE,
            MutationKind.WRITE,
            RateLimitScope.IP,
            responseBody = ResponseBody.EMPTY,
            success = F95ApiStatus.ACCEPTED,
            errors = setOf(F95ApiStatus.FORBIDDEN, F95ApiStatus.TOO_MANY_REQUESTS, F95ApiStatus.INTERNAL_SERVER_ERROR),
            emptyResponses = setOf(F95ApiStatus.FORBIDDEN)
        )
    )

    val endpointContract: List<String> = endpointDescriptors.map(EndpointDescriptor::path).distinct()

    private fun endpoint(
        path: String,
        method: ApiMethod,
        auth: AuthRequirement,
        mutation: MutationKind,
        rateLimit: RateLimitScope,
        request: String? = null,
        response: String = "JsonObject",
        responseBody: ResponseBody = ResponseBody.JSON,
        query: List<String> = emptyList(),
        pathParams: List<String> = emptyList(),
        success: F95ApiStatus = F95ApiStatus.OK,
        errors: Set<F95ApiStatus> = F95ApiStatus.DEFAULT_ERROR_STATUSES,
        extraStatuses: Set<F95ApiStatus> = emptySet(),
        emptyResponses: Set<F95ApiStatus> = emptySet()
    ) = EndpointDescriptor(
        path,
        setOf(method),
        auth,
        mutation,
        rateLimit,
        success,
        responseBody,
        response,
        request,
        query,
        pathParams,
        errors + extraStatuses,
        emptyResponses
    )
}
