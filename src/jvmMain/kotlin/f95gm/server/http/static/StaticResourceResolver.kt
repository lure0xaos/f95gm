package f95gm.server.http.static

import f95gm.server.cache.CacheStatisticsSnapshot
import f95gm.server.cache.CacheNamespaces
import f95gm.server.cache.CacheStatisticsSource
import f95gm.server.cache.CompositeCacheStatisticsProvider

/** Resolves override resources first, then classpath resources, without owning HTTP response behavior. */
internal class StaticResourceResolver(
    overrideDirectory: String?,
    private val fileSystem: StaticFileSystem = NioStaticFileSystem
) {
    private val overrideRoot = overrideDirectory?.let { resolveOverrideRoot(it, fileSystem) }
    private val overrideResources = OverrideResourceCache()
    private val classpathResources = ClasspathResourceCache()
    private val cacheStatistics = CompositeCacheStatisticsProvider(
        listOf(
            CacheStatisticsSource(CacheNamespaces.CLASSPATH, classpathResources),
            CacheStatisticsSource(CacheNamespaces.STATIC, overrideResources)
        ),
        qualifyNamespaces = false
    )

    fun resolve(path: StaticResourcePath): StaticResourceLoadResult = when (val override = overrideResult(path.value)) {
        is StaticResourceLoadResult.Found -> override
        is StaticResourceLoadResult.Failed -> override
        StaticResourceLoadResult.Missing -> when (val classpath = classpathResult(path.value)) {
            is StaticResourceLoadResult.Found -> classpath
            is StaticResourceLoadResult.Failed -> classpath
            StaticResourceLoadResult.Missing -> StaticResourceLoadResult.Missing
        }
    }

    fun snapshot(): CacheStatisticsSnapshot = cacheStatistics.snapshot()

    fun clear() {
        overrideResources.clear()
        classpathResources.clear()
    }

    private fun overrideResult(requested: String): StaticResourceLoadResult = when (val root = overrideRoot) {
        null, StaticFileResolution.Missing -> StaticResourceLoadResult.Missing
        is StaticFileResolution.Failed -> StaticResourceLoadResult.Failed(root.failure.toResourceFailure())
        is StaticFileResolution.Found -> when (val opened = fileSystem.openFile(root.path, requested)) {
            is StaticFileOpenResult.Opened -> try {
                opened.file.use { file -> StaticResourceLoadResult.Found(overrideResources.getOrLoad(file)) }
            } catch (_: java.io.IOException) {
                StaticResourceLoadResult.Failed(StaticResourceFailure.IO)
            } catch (_: SecurityException) {
                StaticResourceLoadResult.Failed(StaticResourceFailure.SECURITY)
            }
            StaticFileOpenResult.Missing -> StaticResourceLoadResult.Missing
            is StaticFileOpenResult.Failed -> StaticResourceLoadResult.Failed(opened.failure.toResourceFailure())
        }
    }

    private fun classpathResult(path: String): StaticResourceLoadResult = try {
        classpathResources.get(path)?.let(StaticResourceLoadResult::Found) ?: StaticResourceLoadResult.Missing
    } catch (_: java.io.IOException) {
        StaticResourceLoadResult.Failed(StaticResourceFailure.IO)
    } catch (_: SecurityException) {
        StaticResourceLoadResult.Failed(StaticResourceFailure.SECURITY)
    }

    private fun StaticFileFailure.toResourceFailure(): StaticResourceFailure = when (this) {
        StaticFileFailure.INVALID_PATH, StaticFileFailure.IO -> StaticResourceFailure.IO
        StaticFileFailure.SECURITY -> StaticResourceFailure.SECURITY
    }
}
