package f95gm.server.http.static

import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.config.security.ServerSecurityConfig
import f95gm.server.http.response.respond
import f95gm.server.state.runtime.CloseGuard
import f95gm.server.state.runtime.LifecycleResource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.readBytes

internal class StaticResourceCache(overrideDirectory: String? = null) : LifecycleResource {
    private val closeGuard = CloseGuard()
    private val overrideResourceCache = OverrideResourceCache()
    private val classpathResourceCache = ClasspathResourceCache()
    private val overrideRoot = overrideDirectory?.let(::resolveOverrideRoot)
    internal val generatedAssetManifest: Set<String> by lazy {
        Thread.currentThread().contextClassLoader
            .getResourceAsStream("static/asset-manifest.json")
            ?.bufferedReader()
            ?.use { reader -> runCatching { Json.parseToJsonElement(reader.readText()).jsonObject.keys }.getOrNull() }
            .orEmpty()
    }

    fun overrideResource(file: Path): StaticResource = overrideResourceCache.getOrLoad(file)

    fun overrideResource(requested: String): StaticResource? =
        overrideRoot?.let { root -> resolveOverrideFile(root, requested) }?.let(::overrideResource)

    fun classpathResource(path: String): StaticResource? = classpathResourceCache.get(path)

    override val isClosed: Boolean get() = closeGuard.isClosed

    override fun close() = closeGuard.closeOnce {
        overrideResourceCache.clear()
        classpathResourceCache.clear()
    }
}

internal suspend fun static(
    call: ApplicationCall,
    requestedPath: String,
    cache: StaticResourceCache,
) {
    val requested = safeStaticPath(requestedPath)
    if (requested == null) {
        respond(
            call,
            ServerConstants.STATUS_NOT_FOUND,
            UiMessages.server_resourceNotFound(),
            ContentType.Text.Plain.toString()
        )
        return
    }
    val resource = cache.overrideResource(requested)
        ?: cache.classpathResource(requested)
    if (resource == null) {
        respond(
            call,
            ServerConstants.STATUS_NOT_FOUND,
            UiMessages.server_resourceNotFound(),
            ContentType.Text.Plain.toString()
        )
        return
    }
    val contentType = staticContentType(requested.substringAfterLast('.', ""))
    val hasRequestNonce =
        requested == "index.html" && call.attributes.getOrNull(ServerSecurityConfig.CSP_NONCE_ATTRIBUTE) != null
    if (!hasRequestNonce) call.response.headers.append(HttpHeaders.ETag, resource.etag)
    val cacheControl = if (hasRequestNonce) "no-store" else staticCacheControl(requested, cache.generatedAssetManifest)
    call.response.headers.append(HttpHeaders.CacheControl, cacheControl)
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

internal data class StaticResource(val bytes: ByteArray, val etag: String)
private data class CachedOverride(val size: Long, val modifiedAt: Long, val resource: StaticResource)

private class OverrideResourceCache(private val maxEntries: Int = 256) {
    private val values = LinkedHashMap<Path, CachedOverride>(maxEntries, 0.75f, true)
    private val lock = Any()

    fun getOrLoad(file: Path): StaticResource = synchronized(lock) {
        val size = file.fileSize()
        val modifiedAt = file.getLastModifiedTime().toMillis()
        values[file]?.takeIf { it.size == size && it.modifiedAt == modifiedAt }?.let { return@synchronized it.resource }
        val resource = file.readBytes().let { bytes -> StaticResource(bytes, etag(bytes)) }
        values[file] = CachedOverride(size, modifiedAt, resource)
        while (values.size > maxEntries) values.entries.iterator().apply { next(); remove() }
        resource
    }

    fun clear() = synchronized(lock) { values.clear() }
}

internal fun etag(bytes: ByteArray): String =
    "\"${MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }}\""

internal fun safeStaticPath(requestedPath: String): String? {
    val requested = requestedPath.ifBlank { "index.html" }
    if (requested.any { it == '\u0000' }) return null
    if (requested.contains("%2e", ignoreCase = true) || requested.contains("%2f", ignoreCase = true) ||
        requested.contains("%5c", ignoreCase = true)
    ) return null
    val normalized = try {
        Path.of(requested).normalize()
    } catch (_: InvalidPathException) {
        return null
    } catch (_: SecurityException) {
        return null
    }
    if (normalized.isAbsolute || normalized.startsWith("..")) return null
    return normalized.toString().replace('\\', '/')
}
