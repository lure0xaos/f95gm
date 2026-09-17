package f95gm.server.http.request

import f95gm.domain.api.EndpointDescriptor
import f95gm.domain.api.ApiPathTemplate
import f95gm.domain.api.F95ApiPaths
import f95gm.domain.api.endpointDescriptors
import io.ktor.http.*

internal class ApiEndpointResolver(
    descriptors: List<EndpointDescriptor> = endpointDescriptors
) {
    private val descriptors = descriptors
    private val exactDescriptors = descriptors.flatMap { descriptor ->
        descriptor.methods.map { method -> method.wireValue to (ApiPathTemplate.parse(descriptor.path.value) to descriptor) }
    }.groupBy({ it.first }, { it.second })

    fun resolve(path: String, method: HttpMethod): EndpointDescriptor? =
        exactDescriptors[method.value].orEmpty().firstOrNull { (template, _) -> template.matches(path) }?.second

    fun isApiPath(path: String): Boolean = path == F95ApiPaths.BASE || path.startsWith("${F95ApiPaths.BASE}/")

}
