package f95gm.domain.api

private val endpointDescriptorInventory: List<EndpointDescriptor> = buildList {
    addAll(authenticationEndpointDescriptors)
    addAll(catalogEndpointDescriptors)
    addAll(markReadEndpointDescriptors)
    addAll(filterEndpointDescriptors)
    addAll(markMutationEndpointDescriptors)
    addAll(operationalEndpointDescriptors)
}

internal data class ApiOperationKey(val path: ApiPath, val method: ApiMethod)

private val validatedEndpointDescriptorInventory: List<EndpointDescriptor> = endpointDescriptorInventory
    .also(::validateEndpointDescriptors)

internal val endpointOperationIndex: Map<ApiOperationKey, EndpointDescriptor> = validatedEndpointDescriptorInventory.flatMap { descriptor ->
        descriptor.methods.map { ApiOperationKey(descriptor.path, it) to descriptor }
    }.toMap()

/** Immutable endpoint inventory shared by routing, OpenAPI generation, and client checks. */
val endpointDescriptors: List<EndpointDescriptor>
    get() = validatedEndpointDescriptorInventory

internal fun validateEndpointDescriptors(descriptors: List<EndpointDescriptor>) {
    require(descriptors.all { it.path.value.startsWith(F95ApiPaths.BASE + "/") }) {
        "Every endpoint must use the versioned API prefix"
    }
    val keys = descriptors.flatMap { descriptor -> descriptor.methods.map { descriptor.path to it } }
    require(keys.size == keys.toSet().size) { "Endpoint method/path keys must be unique" }
    val operationIds = descriptors.flatMap { descriptor -> descriptor.methods.map(descriptor::operationId) }
    require(operationIds.size == operationIds.toSet().size) { "API operation IDs must be unique" }
    require(descriptors.all { it.methods.isNotEmpty() }) { "Every endpoint must declare at least one method" }
    require(descriptors.all { it.apiVersion.value.isNotBlank() }) { "Every endpoint must declare an API version" }
    require(descriptors.all { descriptor ->
        val parameters = descriptor.queryParameters.map(ApiQueryParameter::value) +
            descriptor.pathParameters.map(ApiPathParameter::value)
        parameters.size == parameters.toSet().size && parameters.all(String::isNotBlank)
    }) { "Endpoint parameter names must be unique and non-blank" }
    require(descriptors.all { descriptor ->
        val placeholders = Regex("\\{([^}]+)\\}").findAll(descriptor.path.value).map { it.groupValues[1] }.toList()
        placeholders == descriptor.pathParameters.map(ApiPathParameter::value)
    }) { "Endpoint path placeholders must match declared path parameters in order" }
    require(descriptors.all { descriptor ->
        descriptor.errorStatuses.none { it == descriptor.successStatus } &&
            descriptor.emptyResponseStatuses.all { it in descriptor.errorStatuses || it == descriptor.successStatus }
    }) { "Endpoint response statuses must not overlap or be undocumented" }
    require(descriptors.all { descriptor ->
        descriptor.deprecation == null || descriptor.deprecation.replacement.value.isNotBlank()
    }) { "Deprecated endpoints must declare a replacement path" }
    descriptors.forEach { descriptor ->
        descriptor.queryParameters.forEach { parameterDescriptor(it) }
        descriptor.pathParameters.forEach { parameterDescriptor(it) }
    }
}

val endpointContract: List<String>
    get() = endpointDescriptors.map { it.path.value }.distinct()
