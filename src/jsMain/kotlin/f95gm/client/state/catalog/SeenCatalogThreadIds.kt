package f95gm.client.state.catalog

import f95gm.client.model.catalog.Game
import f95gm.client.state.runtime.BrowserStorage
import f95gm.client.state.runtime.StorageKey
import f95gm.domain.values.F95ThreadId

private val SEEN_STORAGE_KEY = StorageKey("f95gm.catalog.seenThreadIds")
private const val SEEN_SEPARATOR = "|"
private const val MAX_SEEN_IDS = 5_000

internal class SeenCatalogThreadIds(private val storage: BrowserStorage) {

    fun markNew(items: List<Game>): List<Game> {
        val stored = load().toMutableList()
        val seen = stored.toSet()
        val hasPreviousVisit = seen.isNotEmpty()
        val marked = items.map { game ->
            game.copy(isNew = game.isNew || (hasPreviousVisit && game.threadId.value !in seen))
        }
        remember(stored, items.map { it.threadId })
        return marked
    }

    fun markNew(threadId: F95ThreadId, upstreamNew: Boolean): Boolean {
        if (upstreamNew) return true
        val stored = load()
        return stored.isNotEmpty() && threadId.value !in stored
    }

    fun rememberThreadIds(threadIds: Iterable<F95ThreadId>) {
        remember(load().toMutableList(), threadIds)
    }

    private fun load(): List<String> = storage.readOrNull(SEEN_STORAGE_KEY)
        ?.split(SEEN_SEPARATOR)
        ?.filter { it.isNotBlank() }
        .orEmpty()

    private fun remember(stored: MutableList<String>, threadIds: Iterable<F95ThreadId>) {
        val known = stored.toMutableSet()
        threadIds.forEach { threadId ->
            if (known.add(threadId.value)) stored += threadId.value
        }
        storage.writeBestEffort(SEEN_STORAGE_KEY, stored.takeLast(MAX_SEEN_IDS).joinToString(SEEN_SEPARATOR))
    }
}
