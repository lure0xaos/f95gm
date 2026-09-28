package f95gm.server.http.static

import f95gm.server.http.request.parseJsonElementResult
import f95gm.server.model.ParseResult
import kotlinx.serialization.json.JsonObject
import java.io.IOException

internal sealed interface StaticAssetManifestResult {
    data class Loaded(val paths: Set<String>) : StaticAssetManifestResult
    data object Missing : StaticAssetManifestResult
    data object Invalid : StaticAssetManifestResult
}

internal object StaticAssetManifestLoader {
    fun load(classLoader: ClassLoader): StaticAssetManifestResult {
        return try {
            val stream = classLoader.getResourceAsStream("static/asset-manifest.json")
                ?: return StaticAssetManifestResult.Missing
            when (val parsed = stream.bufferedReader().use { parseJsonElementResult(it.readText()) }) {
                is ParseResult.Invalid -> StaticAssetManifestResult.Invalid
                is ParseResult.Success -> (parsed.value as? JsonObject)?.let {
                    StaticAssetManifestResult.Loaded(it.keys.toSet())
                } ?: StaticAssetManifestResult.Invalid
            }
        } catch (_: IOException) {
            StaticAssetManifestResult.Invalid
        }
    }
}
