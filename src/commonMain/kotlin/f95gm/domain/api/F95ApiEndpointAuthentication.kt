package f95gm.domain.api

internal val authenticationEndpointDescriptors = listOf(
    ApiEndpointDefinition(
        path = F95ApiPaths.LOGIN, method = ApiMethod.POST, auth = AuthRequirement.NONE, mutation = MutationKind.WRITE,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.SESSION, request = ApiSchema.LOGIN_REQUEST,
        summary = "Create a browser session",
        description = "Authenticates the user and establishes a browser session.",
        errors = setOf(F95ApiStatus.BAD_REQUEST, F95ApiStatus.UNAUTHORIZED, F95ApiStatus.TOO_MANY_REQUESTS,
            F95ApiStatus.INTERNAL_SERVER_ERROR, F95ApiStatus.BAD_GATEWAY, F95ApiStatus.SERVICE_UNAVAILABLE,
            F95ApiStatus.GATEWAY_TIMEOUT)
    ),
    ApiEndpointDefinition(
        path = F95ApiPaths.LOGOUT, method = ApiMethod.POST, auth = AuthRequirement.SESSION, mutation = MutationKind.WRITE,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.SESSION, request = ApiSchema.EMPTY_REQUEST,
        summary = "End the current browser session",
        description = "Ends the current browser session and clears its upstream session state."
    ),
    ApiEndpointDefinition(
        path = F95ApiPaths.SESSION, method = ApiMethod.GET, auth = AuthRequirement.NONE, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.SESSION, response = ApiSchema.OK_RESPONSE,
        summary = "Check the current browser session",
        description = "Verifies the browser session against the upstream and returns an API error when no valid session exists.",
        errors = setOf(F95ApiStatus.UNAUTHORIZED, F95ApiStatus.TOO_MANY_REQUESTS,
            F95ApiStatus.INTERNAL_SERVER_ERROR, F95ApiStatus.BAD_GATEWAY, F95ApiStatus.SERVICE_UNAVAILABLE,
            F95ApiStatus.GATEWAY_TIMEOUT)
    )
).map(ApiEndpointDefinition::toDescriptor)
