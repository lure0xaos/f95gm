package f95gm.client.ui.catalog.toolbar

import f95gm.domain.filters.F95Sort
import f95gm.messages.UiMessages

internal fun catalogSortLabel(sort: F95Sort): String = when (sort) {
    F95Sort.DATE -> UiMessages.app_sortDate()
    F95Sort.DATE_ASC -> UiMessages.app_sortDateOldest()
    F95Sort.LIKES_ASC -> UiMessages.app_sortLikesLeast()
    F95Sort.VIEWS_ASC -> UiMessages.app_sortViewsLeast()
    F95Sort.TITLE -> UiMessages.app_sortName()
    F95Sort.TITLE_DESC -> UiMessages.app_sortNameZA()
    F95Sort.RATING -> UiMessages.app_sortRating()
    F95Sort.RATING_ASC -> UiMessages.app_sortRatingLowest()
    F95Sort.LIKES -> UiMessages.app_sortLikes()
    F95Sort.VIEWS -> UiMessages.app_sortViews()
}
