package f95gm.server.app

import f95gm.domain.api.F95ApiPaths
import f95gm.domain.api.ApiOperationKey
import f95gm.domain.api.ApiOperationId
import f95gm.domain.api.ApiPath
import f95gm.domain.api.ApiMethod
import f95gm.domain.api.EndpointDescriptor
import f95gm.domain.api.endpointDescriptors
import f95gm.domain.api.operationId
import io.ktor.server.routing.*
import f95gm.server.kernel.ServerInvariantViolation

internal sealed interface ApiRouteValidationResult {
    data object Valid : ApiRouteValidationResult
    data class Invalid(val message: String) : ApiRouteValidationResult
}

internal data class ConfiguredApiEndpoint(
    val descriptor: EndpointDescriptor,
    val method: ApiMethod,
    val register: Route.(ApiPath) -> Unit
) {
    val path: String get() = descriptor.path.value
    val operationId: ApiOperationId get() = descriptor.operationId(method)
    val contractDescriptor: EndpointDescriptor get() = descriptor

    init {
        when (val result = validateConfiguredApiEndpoint(descriptor, method)) {
            ApiRouteValidationResult.Valid -> Unit
            is ApiRouteValidationResult.Invalid -> throw ServerInvariantViolation(result.message)
        }
    }
}

private fun validateConfiguredApiEndpoint(
    descriptor: EndpointDescriptor,
    method: ApiMethod
): ApiRouteValidationResult = if (method in descriptor.methods) {
    ApiRouteValidationResult.Valid
} else {
    ApiRouteValidationResult.Invalid("Route $method ${descriptor.path} is not declared in the shared API contract")
}

internal fun configuredEndpoint(
    operation: ApiOperationKey,
    register: Route.(ApiPath) -> Unit
): ConfiguredApiEndpoint {
    val generated = GeneratedServerApiRegistry.require(operation.method, operation.path)
    val match = endpointDescriptors.asSequence()
        .flatMap { descriptor -> descriptor.methods.asSequence().map { method -> descriptor to method } }
        .single { (descriptor, method) -> descriptor.path == operation.path && method == operation.method }
    return ConfiguredApiEndpoint(
        descriptor = match.first,
        method = generated.method,
        register = register
    ).also {
        check(it.path == generated.path.value) {
            "Generated route metadata disagrees with shared endpoint metadata for $operation"
        }
    }
}

internal val configuredApiEndpoints: List<ConfiguredApiEndpoint> get() = apiRouteHandlers

private val configuredApiEndpointKeys = configuredApiEndpoints.map { it.operationId }.toSet()
private val delegatedEndpointPaths = setOf(F95ApiPaths.HEALTH, F95ApiPaths.LIVE, F95ApiPaths.READY, F95ApiPaths.METRICS, F95ApiPaths.SHUTDOWN)
private val delegatedApiEndpointKeys = endpointDescriptors.filter { it.path.value in delegatedEndpointPaths }.flatMap { descriptor ->
    descriptor.methods.map { descriptor.operationId(it) }
}.toSet()
private val declaredApiEndpointKeys = endpointDescriptors.filterNot { it.path.value in delegatedEndpointPaths }.flatMap { descriptor ->
    descriptor.methods.map { descriptor.operationId(it) }
}.toSet()
private val generatedApiEndpointKeys = GeneratedServerApiRegistry.operations.map { it.id }.toSet()

private fun validateConfiguredApiEndpoints(): ApiRouteValidationResult {
    if (configuredApiEndpointKeys + delegatedApiEndpointKeys != generatedApiEndpointKeys) {
        return ApiRouteValidationResult.Invalid(
            "Configured API routes must exactly match the generated server operation registry: " +
                "missing=${generatedApiEndpointKeys - (configuredApiEndpointKeys + delegatedApiEndpointKeys)}, " +
                "extra=${(configuredApiEndpointKeys + delegatedApiEndpointKeys) - generatedApiEndpointKeys}"
        )
    }
    if (configuredApiEndpointKeys != declaredApiEndpointKeys) {
        return ApiRouteValidationResult.Invalid(
            "Configured API routes must exactly match the shared endpoint registry: " +
                "missing=${declaredApiEndpointKeys - configuredApiEndpointKeys}, " +
                "extra=${configuredApiEndpointKeys - declaredApiEndpointKeys}"
        )
    }
    return ApiRouteValidationResult.Valid
}

private val validatedConfiguredApiEndpoints: List<ConfiguredApiEndpoint> = configuredApiEndpoints.also {
    when (val result = validateConfiguredApiEndpoints()) {
        ApiRouteValidationResult.Valid -> Unit
        is ApiRouteValidationResult.Invalid -> throw ServerInvariantViolation(result.message)
    }
}

internal fun Route.configureApiRoutes() {
    validatedConfiguredApiEndpoints.forEach { it.register(this, it.descriptor.path) }
}
