package f95gm.client.routing.pages

import f95gm.domain.values.F95ThreadId

internal fun itemThreadId(path: String): F95ThreadId? {
    val normalized = path.removeSuffix("/")
    if (!normalized.startsWith("/item/")) return null
    val threadId = normalized.removePrefix("/item/")
    return threadId.takeIf { it.isNotEmpty() && it.all { character -> character in '0'..'9' } }
        ?.let(::F95ThreadId)
}
