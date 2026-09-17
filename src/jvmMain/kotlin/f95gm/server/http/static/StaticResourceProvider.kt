package f95gm.server.http.static

/** Source boundary used by HTTP mapping; it hides filesystem/classpath selection. */
internal interface StaticResourceProvider {
    fun resolve(path: StaticResourcePath): StaticResourceLoadResult
    fun assetManifestPaths(): Set<String>
}

internal sealed interface StaticResourceLoadResult {
    data class Found(val resource: StaticResource) : StaticResourceLoadResult
    data object Missing : StaticResourceLoadResult
    data class Failed(val reason: StaticResourceFailure) : StaticResourceLoadResult
}

internal enum class StaticResourceFailure { IO, SECURITY }
