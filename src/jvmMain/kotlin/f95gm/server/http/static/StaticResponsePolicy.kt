package f95gm.server.http.static

import f95gm.server.config.security.ServerSecurityConfig
import f95gm.server.http.response.appendNoStore
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respond

internal suspend fun respondStaticResource(call: ApplicationCall, path: StaticResourcePath, resource: StaticResource, manifest: Set<String>) {
    val contentType = staticContentType(path.value.substringAfterLast('.', ""))
    val hasRequestNonce = path.value == "index.html" && call.attributes.getOrNull(ServerSecurityConfig.CSP_NONCE_ATTRIBUTE) != null
    if (!hasRequestNonce) call.response.headers.append(HttpHeaders.ETag, resource.etag)
    val cacheControl = if (hasRequestNonce) "no-store" else staticCacheControl(path.value, manifest)
    if (cacheControl == "no-store") call.appendNoStore()
    else call.response.headers.append(HttpHeaders.CacheControl, cacheControl)
    if (!hasRequestNonce && call.request.headers[HttpHeaders.IfNoneMatch] == resource.etag) {
        call.respond(HttpStatusCode.NotModified)
        return
    }
    val body = if (hasRequestNonce) {
        String(resource.bytes, Charsets.UTF_8)
            .replace("__F95GM_CSP_NONCE__", call.attributes[ServerSecurityConfig.CSP_NONCE_ATTRIBUTE])
            .toByteArray(Charsets.UTF_8)
    } else resource.bytes
    call.respondBytes(body, contentType = contentType)
}
