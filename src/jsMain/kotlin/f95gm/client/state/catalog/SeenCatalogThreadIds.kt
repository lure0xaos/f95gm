package f95gm.client.state.catalog

import f95gm.client.model.catalog.Game
import kotlinx.browser.window

internal object SeenCatalogThreadIds {
    private const val STORAGE_KEY = "f95gm.catalog.seenThreadIds"
    private const val SEPARATOR = "|"
    private const val MAX_IDS = 5_000

    fun markNew(items: List<Game>): List<Game> {
        val stored = load().toMutableList()
        val seen = stored.toSet()
        val hasPreviousVisit = seen.isNotEmpty()
        val marked = items.map { game ->
            game.copy(isNew = game.isNew || (hasPreviousVisit && game.threadId.value !in seen))
        }
        remember(stored, items.map { it.threadId.value })
        return marked
    }

    fun markNew(threadId: String, upstreamNew: Boolean): Boolean =
        upstreamNew || (load().isNotEmpty() && threadId !in load())

    fun rememberThreadIds(threadIds: Iterable<String>) {
        remember(load().toMutableList(), threadIds)
    }

    private fun load(): List<String> = window.localStorage.getItem(STORAGE_KEY)
        ?.split(SEPARATOR)
        ?.filter { it.isNotBlank() }
        .orEmpty()

    private fun remember(stored: MutableList<String>, threadIds: Iterable<String>) {
        val known = stored.toMutableSet()
        threadIds.forEach { threadId ->
            if (known.add(threadId)) stored += threadId
        }
        window.localStorage.setItem(STORAGE_KEY, stored.takeLast(MAX_IDS).joinToString(SEPARATOR))
    }
}
