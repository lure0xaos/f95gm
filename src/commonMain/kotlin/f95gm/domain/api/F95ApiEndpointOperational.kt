package f95gm.domain.api

internal val operationalEndpointDescriptors = listOf(
    ApiEndpointDefinition(
        path = F95ApiPaths.ITEM_BY_THREAD, method = ApiMethod.GET, auth = AuthRequirement.SESSION, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.ITEMS, response = ApiSchema.THREAD_DETAIL,
        pathParams = listOf(ApiPathParameter(F95ApiPaths.THREAD_ID)), summary = "Load release details",
        description = "Returns parsed details for one release thread."),
    ApiEndpointDefinition(
        path = F95ApiPaths.MEDIA, method = ApiMethod.GET, auth = AuthRequirement.SESSION, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.ITEMS, responseBody = ResponseBody.BINARY,
        response = ApiSchema.MEDIA, query = listOf(F95ApiQueryParameters.URL),
        summary = "Load proxied release media",
        description = "Streams an allowed upstream media resource as binary content."
    ),
    ApiEndpointDefinition(
        path = F95ApiPaths.HEALTH, method = ApiMethod.GET, auth = AuthRequirement.NONE, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.NONE, tag = ApiTag.SERVER, responseBody = ResponseBody.EMPTY, errors = emptySet(),
        query = listOf(F95ApiQueryParameters.PROBE, F95ApiQueryParameters.TAB_ID, F95ApiQueryParameters.THEME),
        summary = "Check server health",
        description = "Reports whether the server process is accepting health checks."),
    ApiEndpointDefinition(
        path = F95ApiPaths.LIVE, method = ApiMethod.GET, auth = AuthRequirement.NONE, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.NONE, tag = ApiTag.SERVER, responseBody = ResponseBody.EMPTY, errors = emptySet(), summary = "Check server liveness",
        description = "Reports whether the server process is alive."),
    ApiEndpointDefinition(
        path = F95ApiPaths.READY, method = ApiMethod.GET, auth = AuthRequirement.NONE, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.NONE, tag = ApiTag.SERVER, responseBody = ResponseBody.EMPTY, errors = emptySet(),
        extraStatuses = setOf(F95ApiStatus.SERVICE_UNAVAILABLE), emptyResponses = setOf(F95ApiStatus.SERVICE_UNAVAILABLE),
        summary = "Check server readiness",
        description = "Returns an empty 200 response when ready, or an empty 503 response while draining."),
    ApiEndpointDefinition(
        path = F95ApiPaths.METRICS, method = ApiMethod.GET, auth = AuthRequirement.NONE, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.NONE, tag = ApiTag.SERVER, response = ApiSchema.REQUEST_METRICS, errors = emptySet(), summary = "Read server request metrics",
        description = "Returns request, retry, cache, and circuit-breaker metrics.") ,
    ApiEndpointDefinition(
        path = F95ApiPaths.SHUTDOWN, method = ApiMethod.POST, auth = AuthRequirement.NONE, mutation = MutationKind.WRITE,
        rateLimit = RateLimitScope.IP, tag = ApiTag.SERVER, responseBody = ResponseBody.EMPTY, success = F95ApiStatus.ACCEPTED,
        errors = setOf(F95ApiStatus.FORBIDDEN, F95ApiStatus.TOO_MANY_REQUESTS, F95ApiStatus.INTERNAL_SERVER_ERROR),
        emptyResponses = setOf(F95ApiStatus.FORBIDDEN), summary = "Request server shutdown",
        description = "Returns an empty 202 response when the shutdown request is accepted.")
).map(ApiEndpointDefinition::toDescriptor)
