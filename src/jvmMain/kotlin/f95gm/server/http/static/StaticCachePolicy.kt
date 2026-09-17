package f95gm.server.http.static

internal fun staticCacheControl(requested: String, generatedAssets: Set<String>): String = when {
    requested == "index.html" -> "no-cache"
    requested in generatedAssets -> "public, max-age=31536000, immutable"
    else -> "public, max-age=0, must-revalidate"
}
