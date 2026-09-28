package f95gm.domain.api

/** Location is part of a parameter's identity: query and path keys cannot collide. */
private data class ApiParameterKey(val name: String, val location: ParameterLocation)

private val parameterDescriptors: Map<ApiParameterKey, ParameterDescriptor> = listOf(
    ParameterDescriptor(F95ApiPaths.FILTER_ID, ParameterType.STRING, ParameterLocation.PATH),
    ParameterDescriptor(F95ApiPaths.THREAD_ID, ParameterType.STRING, ParameterLocation.PATH),
    ParameterDescriptor(F95ApiQueryParameters.CATEGORY.value, ParameterType.STRING, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.PROBE.value, ParameterType.INTEGER, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.TAB_ID.value, ParameterType.STRING, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.THEME.value, ParameterType.STRING, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.PAGE.value, ParameterType.INTEGER, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.ROWS.value, ParameterType.INTEGER, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.SORT.value, ParameterType.STRING, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.SEARCH.value, ParameterType.STRING, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.CREATOR.value, ParameterType.STRING, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.DATE.value, ParameterType.INTEGER, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.TAG_TYPE.value, ParameterType.STRING, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.TAGS.value, ParameterType.STRING_ARRAY, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.EXCLUDED_TAGS.value, ParameterType.STRING_ARRAY, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.PREFIXES.value, ParameterType.STRING_ARRAY, ParameterLocation.QUERY),
    ParameterDescriptor(F95ApiQueryParameters.EXCLUDED_PREFIXES.value, ParameterType.STRING_ARRAY, ParameterLocation.QUERY),
    ParameterDescriptor(
        F95ApiQueryParameters.URL.value, ParameterType.STRING, ParameterLocation.QUERY
    )
).associateBy { ApiParameterKey(it.name, it.location) }

fun parameterDescriptor(parameter: ApiQueryParameter): ParameterDescriptor =
    parameterDescriptor(ApiParameterKey(parameter.value, ParameterLocation.QUERY))

fun parameterDescriptor(parameter: ApiPathParameter): ParameterDescriptor =
    parameterDescriptor(ApiParameterKey(parameter.value, ParameterLocation.PATH))

private fun parameterDescriptor(key: ApiParameterKey): ParameterDescriptor =
    parameterDescriptors[key] ?: throw ApiContractInvariantViolation(
        "Missing API parameter descriptor for ${key.location.name.lowercase()} parameter '${key.name}'"
    )
