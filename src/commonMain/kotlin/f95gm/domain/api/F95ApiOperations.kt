package f95gm.domain.api

internal fun EndpointDescriptor.operationId(method: ApiMethod): ApiOperationId =
    ApiOperationId("${method.wireValue.lowercase()}_${path.value.trim('/').replace('/', '_').replace('{', '_').replace('}', '_')}")

data class ApiOperation(
    val operationId: ApiOperationId,
    val path: ApiPath,
    val method: ApiMethod
)

fun operation(path: String, method: ApiMethod): ApiOperation = operation(apiPath(path), method)

fun operation(path: ApiPath, method: ApiMethod): ApiOperation = endpointOperationIndex
    .get(ApiOperationKey(path, method))
    ?.let { ApiOperation(it.operationId(method), path, method) }
    ?: throw ApiContractInvariantViolation("Missing API operation for ${method.wireValue} $path")

fun operationId(path: String, method: ApiMethod): ApiOperationId = operationId(apiPath(path), method)

fun operationId(path: ApiPath, method: ApiMethod): ApiOperationId = endpointOperationIndex
    .get(ApiOperationKey(path, method))
    ?.operationId(method)
    ?: throw ApiContractInvariantViolation("Missing API operation for ${method.wireValue} $path")
