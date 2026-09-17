package f95gm.domain.api

internal val catalogEndpointDescriptors = listOf(
    ApiEndpointDefinition(
        path = F95ApiPaths.CATALOG, method = ApiMethod.GET, auth = AuthRequirement.SESSION, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.CATALOG, responseBody = ResponseBody.TEXT,
        response = ApiSchema.UPSTREAM_CATALOG,
        summary = "Load catalog releases",
        description = "Returns the upstream catalog payload as received by the server; the upstream may choose JSON or text.",
        query = listOf(F95ApiQueryParameters.CATEGORY, F95ApiQueryParameters.PAGE, F95ApiQueryParameters.ROWS, F95ApiQueryParameters.SORT, F95ApiQueryParameters.TAGS,
            F95ApiQueryParameters.EXCLUDED_TAGS, F95ApiQueryParameters.PREFIXES, F95ApiQueryParameters.EXCLUDED_PREFIXES, F95ApiQueryParameters.TAG_TYPE,
            F95ApiQueryParameters.DATE, F95ApiQueryParameters.SEARCH, F95ApiQueryParameters.CREATOR),
        errors = setOf(F95ApiStatus.BAD_REQUEST, F95ApiStatus.UNAUTHORIZED, F95ApiStatus.TOO_MANY_REQUESTS,
            F95ApiStatus.INTERNAL_SERVER_ERROR, F95ApiStatus.BAD_GATEWAY, F95ApiStatus.SERVICE_UNAVAILABLE,
            F95ApiStatus.GATEWAY_TIMEOUT)
    ),
    ApiEndpointDefinition(
        path = F95ApiPaths.CATALOG_OPTIONS, method = ApiMethod.GET, auth = AuthRequirement.SESSION, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.CATALOG, response = ApiSchema.CATALOG_OPTIONS,
        summary = "Load catalog filter options",
        description = "Returns the available catalog categories, tags, prefixes, and related filter options.",
        query = listOf(F95ApiQueryParameters.CATEGORY)
    ),
).map(ApiEndpointDefinition::toDescriptor)
