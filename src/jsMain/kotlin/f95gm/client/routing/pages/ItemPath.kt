package f95gm.client.routing.pages

internal fun itemThreadId(path: String): String? {
    val normalized = path.removeSuffix("/")
    if (!normalized.startsWith("/item/")) return null
    val threadId = normalized.removePrefix("/item/")
    return threadId.takeIf { it.isNotEmpty() && it.all { character -> character in '0'..'9' } }
}
