package f95gm.server.http.request

import f95gm.domain.api.F95Api
import io.ktor.http.*

internal class ApiEndpointResolver(
    descriptors: List<F95Api.EndpointDescriptor> = F95Api.endpointDescriptors
) {
    private val descriptors = descriptors

    fun resolve(path: String, method: HttpMethod): F95Api.EndpointDescriptor? =
        descriptors.firstOrNull { descriptor ->
            descriptor.methods.any { it.wireValue == method.value } && matches(descriptor.path, normalizePath(path))
        }

    fun isApiPath(path: String): Boolean = path == F95Api.BASE || path.startsWith("${F95Api.BASE}/")

    private fun matches(template: String, path: String): Boolean {
        val templateParts = template.trim('/').split('/')
        val pathParts = normalizePath(path).trim('/').split('/')
        return templateParts.size == pathParts.size && templateParts.zip(pathParts).all { (expected, actual) ->
            expected.startsWith("{") && expected.endsWith("}") || expected == actual
        }
    }

    private fun normalizePath(path: String): String =
        path.substringBefore('?').let { if (it.length > 1) it.trimEnd('/') else it }
}
