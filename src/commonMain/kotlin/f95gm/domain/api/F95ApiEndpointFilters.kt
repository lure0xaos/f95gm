package f95gm.domain.api

internal val filterEndpointDescriptors = listOf(
    ApiEndpointDefinition(
        path = F95ApiPaths.FILTERS, method = ApiMethod.GET, auth = AuthRequirement.SESSION, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.SAVED_FILTERS, response = ApiSchema.SAVED_FILTERS, summary = "List saved filters",
        description = "Returns the saved filters belonging to the current user."),
    ApiEndpointDefinition(
        path = F95ApiPaths.FILTERS, method = ApiMethod.POST, auth = AuthRequirement.SESSION, mutation = MutationKind.WRITE,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.SAVED_FILTERS, request = ApiSchema.SAVED_FILTER_REQUEST,
        response = ApiSchema.SAVED_FILTERS, summary = "Save a filter",
        description = "Validates and persists a saved filter for the current user."),
    ApiEndpointDefinition(
        path = F95ApiPaths.FILTERS_EXPORT, method = ApiMethod.GET, auth = AuthRequirement.SESSION, mutation = MutationKind.READ,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.SAVED_FILTERS, response = ApiSchema.QUICK_LINKS_BACKUP, summary = "Export saved filters",
        description = "Exports the current user's saved filters as a portable backup document."),
    ApiEndpointDefinition(
        path = F95ApiPaths.FILTERS_IMPORT, method = ApiMethod.POST, auth = AuthRequirement.SESSION, mutation = MutationKind.WRITE,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.SAVED_FILTERS, request = ApiSchema.QUICK_LINKS_BACKUP,
        response = ApiSchema.IMPORTED_QUICK_LINKS, summary = "Import saved filters",
        description = "Imports and validates a saved-filter backup document."),
    ApiEndpointDefinition(
        path = F95ApiPaths.FILTER_BY_ID, method = ApiMethod.DELETE, auth = AuthRequirement.SESSION, mutation = MutationKind.WRITE,
        rateLimit = RateLimitScope.IP_AND_ACCOUNT, tag = ApiTag.SAVED_FILTERS, response = ApiSchema.OK_RESPONSE,
        pathParams = listOf(ApiPathParameter(F95ApiPaths.FILTER_ID)), summary = "Delete a saved filter",
        description = "Deletes one saved filter belonging to the current user.")
).map(ApiEndpointDefinition::toDescriptor)
