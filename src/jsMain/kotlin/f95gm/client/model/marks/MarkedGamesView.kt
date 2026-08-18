package f95gm.client.model.marks

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Sort

internal fun markedGamesView(state: MarksState): MarkedGamesView {
    val search = state.search.trim()
    val filtered = state.items.filter { mark ->
        search.isBlank() ||
                mark.title.value.contains(search, ignoreCase = true) ||
                mark.developer.value.contains(search, ignoreCase = true)
    }
    val sorted = when (state.sort) {
        F95Sort.DATE_ASC.wireValue -> filtered.sortedWith(
            compareBy<MarkedGame> { it.updatedAt }
                .thenBy { it.title.value.lowercase() }
        )

        F95Sort.TITLE.wireValue -> filtered.sortedWith(
            compareBy<MarkedGame> { it.title.value.lowercase() }
                .thenBy { it.developer.value.lowercase() }
        )

        F95Sort.TITLE_DESC.wireValue -> filtered.sortedWith(
            compareByDescending<MarkedGame> { it.title.value.lowercase() }
                .thenByDescending { it.developer.value.lowercase() }
        )

        else -> filtered.sortedWith(
            compareByDescending<MarkedGame> { it.updatedAt }
                .thenBy { it.title.value.lowercase() }
        )
    }
    val totalPages = maxOf(
        F95Defaults.FIRST_PAGE,
        (sorted.size + MARKED_GAMES_PAGE_SIZE - 1) / MARKED_GAMES_PAGE_SIZE
    )
    val page = state.page.coerceIn(F95Defaults.FIRST_PAGE, totalPages)
    val start = (page - F95Defaults.FIRST_PAGE) * MARKED_GAMES_PAGE_SIZE
    return MarkedGamesView(
        items = sorted.drop(start).take(MARKED_GAMES_PAGE_SIZE),
        page = page,
        totalPages = totalPages
    )
}
