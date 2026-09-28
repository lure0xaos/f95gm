package f95gm.domain.api

internal data class ApiEndpointDefinition(
    val path: String,
    val method: ApiMethod,
    val auth: AuthRequirement,
    val mutation: MutationKind,
    val rateLimit: RateLimitScope,
    val tag: ApiTag,
    val request: ApiSchema? = null,
    val response: ApiSchema = ApiSchema.JSON_OBJECT,
    val responseBody: ResponseBody = ResponseBody.JSON,
    val query: List<ApiQueryParameter> = emptyList(),
    val pathParams: List<ApiPathParameter> = emptyList(),
    val summary: String,
    val description: String,
    val success: F95ApiStatus = F95ApiStatus.OK,
    val errors: Set<F95ApiStatus> = F95ApiStatus.DEFAULT_ERROR_STATUSES,
    val extraStatuses: Set<F95ApiStatus> = emptySet(),
    val emptyResponses: Set<F95ApiStatus> = emptySet()
) {
    fun toDescriptor(): EndpointDescriptor = EndpointDescriptor(
        path = apiPath(path),
        methods = setOf(method),
        auth = auth,
        mutation = mutation,
        rateLimit = rateLimit,
        tag = tag,
        successStatus = success,
        responseBody = responseBody,
        responseSchema = response,
        requestSchema = request,
        documentation = ApiOperationDocumentation(summary, description),
        queryParameters = query,
        pathParameters = pathParams,
        errorStatuses = errors + extraStatuses,
        emptyResponseStatuses = emptyResponses
    )
}
