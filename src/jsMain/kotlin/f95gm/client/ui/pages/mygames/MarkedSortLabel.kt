package f95gm.client.ui.pages.mygames

import f95gm.domain.filters.F95Sort
import f95gm.messages.UiMessages

internal fun markedSortLabel(sort: F95Sort): String = when (sort) {
    F95Sort.DATE_ASC -> UiMessages.app_sortDateOldest()
    F95Sort.TITLE -> UiMessages.app_sortName()
    F95Sort.TITLE_DESC -> UiMessages.app_sortNameZA()
    else -> UiMessages.app_sortDate()
}
