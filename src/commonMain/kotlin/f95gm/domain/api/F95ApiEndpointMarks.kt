package f95gm.domain.api

internal val markReadEndpointDescriptors = listOf(
    ApiEndpointDefinition(
        path = F95ApiPaths.MARKS, method = ApiMethod.GET, auth = AuthRequirement.SESSION, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.MARKS, response = ApiSchema.MARKS, summary = "List marked releases",
        description = "Returns the current user's marked releases."),
    ApiEndpointDefinition(
        path = F95ApiPaths.MARKS_CHECK, method = ApiMethod.GET, auth = AuthRequirement.SESSION, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.MARKS, response = ApiSchema.MARKS, summary = "Check marked releases for updates",
        description = "Checks the current user's marked releases against the latest catalog state."),
).map(ApiEndpointDefinition::toDescriptor)

internal val markMutationEndpointDescriptors = listOf(
    ApiEndpointDefinition(
        path = F95ApiPaths.MARK_BY_THREAD, method = ApiMethod.PUT, auth = AuthRequirement.SESSION, mutation = MutationKind.WRITE,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.MARKS, request = ApiSchema.MARK_REQUEST,
        response = ApiSchema.MARKS, pathParams = listOf(ApiPathParameter(F95ApiPaths.THREAD_ID)), summary = "Save a release mark",
        description = "Creates or updates a mark for one release."),
    ApiEndpointDefinition(
        path = F95ApiPaths.MARK_BY_THREAD, method = ApiMethod.DELETE, auth = AuthRequirement.SESSION, mutation = MutationKind.WRITE,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.MARKS, response = ApiSchema.OK_RESPONSE,
        pathParams = listOf(ApiPathParameter(F95ApiPaths.THREAD_ID)), summary = "Delete a release mark",
        description = "Deletes the mark for one release.")
).map(ApiEndpointDefinition::toDescriptor)
