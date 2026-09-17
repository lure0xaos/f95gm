package f95gm.client.ui.shared.pagination

import f95gm.domain.defaults.F95Defaults

/** Common page-boundary rules shared by catalog and marked-games actions. */
internal fun boundedPage(requested: Int, totalPages: Int): Int =
    requested.coerceIn(F95Defaults.FIRST_PAGE, totalPages.coerceAtLeast(F95Defaults.FIRST_PAGE))

internal fun nextPage(current: Int, delta: Int, totalPages: Int): Int =
    boundedPage(current + delta, totalPages)
