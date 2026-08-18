package f95gm.server.app

internal fun isItemDetailPath(path: String): Boolean {
    val normalized = path.removeSuffix("/")
    val threadId = normalized.removePrefix("item/")
    return normalized.startsWith("item/") &&
            threadId.isNotEmpty() &&
            threadId.all { it in '0'..'9' }
}
