package f95gm.server.http.static

import f95gm.messages.UiMessages
import f95gm.server.config.constants.HttpStatusCatalog
import f95gm.server.config.StaticDirectoryPath
import f95gm.server.http.response.respond
import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource
import f95gm.server.kernel.ResourceLock
import f95gm.server.kernel.MetricCounter
import f95gm.server.cache.CacheStatisticsProvider
import io.ktor.http.*
import io.ktor.server.application.*
import java.util.concurrent.atomic.AtomicReference

internal enum class StaticAssetManifestInitializationStatus { UNINITIALIZED, LOADED, MISSING, INVALID }

internal class StaticResourceCache(
    overrideDirectory: StaticDirectoryPath? = null,
    private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader
) : LifecycleResource, StaticResourceProvider, CacheStatisticsProvider {
    private val closeGuard = CloseGuard()
    private val resolver = StaticResourceResolver(overrideDirectory?.value)
    private val assetManifestState = AtomicReference<StaticAssetManifestResult?>(null)
    private val assetManifestLock = ResourceLock()
    private val assetManifestFailures = MetricCounter()

    internal val assetManifestInitializationStatus: StaticAssetManifestInitializationStatus
        get() = assetManifestState.get().status()
    internal val assetManifestFailureCount: Long get() = assetManifestFailures.get()

    override fun snapshot(): f95gm.server.cache.CacheStatisticsSnapshot = resolver.snapshot()

    override fun resolve(path: StaticResourcePath): StaticResourceLoadResult = resolver.resolve(path)

    override fun assetManifestPaths(): Set<String> = assetManifestLock.withLock {
        if (closeGuard.isClosed) return@withLock emptySet()
        val manifest = assetManifestState.get() ?: StaticAssetManifestLoader
            .load(classLoader)
            .also { loaded ->
                assetManifestState.set(loaded)
                if (loaded is StaticAssetManifestResult.Invalid) assetManifestFailures.increment()
            }
        (manifest as? StaticAssetManifestResult.Loaded)?.paths.orEmpty()
    }

    override val isClosed: Boolean get() = closeGuard.isClosed

    override fun close() = closeGuard.closeOnce {
        resolver.clear()
    }
}

private fun StaticAssetManifestResult?.status(): StaticAssetManifestInitializationStatus = when (this) {
    null -> StaticAssetManifestInitializationStatus.UNINITIALIZED
    is StaticAssetManifestResult.Loaded -> StaticAssetManifestInitializationStatus.LOADED
    StaticAssetManifestResult.Missing -> StaticAssetManifestInitializationStatus.MISSING
    StaticAssetManifestResult.Invalid -> StaticAssetManifestInitializationStatus.INVALID
}

internal suspend fun static(
    call: ApplicationCall,
    requestedPath: String,
    cache: StaticResourceProvider,
) {
    val requested = safeStaticPath(requestedPath)
    if (requested == null) {
        respond(
            call,
            HttpStatusCatalog.NOT_FOUND,
            UiMessages.server_resourceNotFound(),
            ContentType.Text.Plain
        )
        return
    }
    val resource = when (val loaded = cache.resolve(requested)) {
        is StaticResourceLoadResult.Found -> loaded.resource
        StaticResourceLoadResult.Missing,
        is StaticResourceLoadResult.Failed -> null
    }
    if (resource == null) {
        respond(
            call,
            HttpStatusCatalog.NOT_FOUND,
            UiMessages.server_resourceNotFound(),
            ContentType.Text.Plain
        )
        return
    }
    val manifest = cache.assetManifestPaths()
    respondStaticResource(call, requested, resource, manifest)
}

internal data class StaticResource(val bytes: ByteArray, val etag: String)
internal sealed interface StaticResourceResult {
    data class Found(val resource: StaticResource) : StaticResourceResult
    data object Missing : StaticResourceResult
}
