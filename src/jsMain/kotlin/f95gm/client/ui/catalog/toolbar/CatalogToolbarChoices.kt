package f95gm.client.ui.catalog.toolbar

import f95gm.client.model.catalog.DropdownChoice
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95Sort
import f95gm.messages.UiMessages

internal fun catalogCategoryChoices() = listOf(
    DropdownChoice(F95Category.GAMES.wireValue, UiMessages.app_games()),
    DropdownChoice(F95Category.COMICS.wireValue, UiMessages.app_comics()),
    DropdownChoice(F95Category.ANIMATIONS.wireValue, UiMessages.app_animations()),
    DropdownChoice(F95Category.ASSETS.wireValue, UiMessages.app_assets()),
    DropdownChoice(F95Category.MODS.wireValue, UiMessages.app_mods())
)

internal fun catalogSortChoices() = listOf(
    DropdownChoice(F95Sort.DATE.wireValue, UiMessages.app_sortDate()),
    DropdownChoice(F95Sort.DATE_ASC.wireValue, UiMessages.app_sortDateOldest()),
    DropdownChoice(F95Sort.LIKES.wireValue, UiMessages.app_sortLikes()),
    DropdownChoice(F95Sort.LIKES_ASC.wireValue, UiMessages.app_sortLikesLeast()),
    DropdownChoice(F95Sort.VIEWS.wireValue, UiMessages.app_sortViews()),
    DropdownChoice(F95Sort.VIEWS_ASC.wireValue, UiMessages.app_sortViewsLeast()),
    DropdownChoice(F95Sort.TITLE.wireValue, UiMessages.app_sortName()),
    DropdownChoice(F95Sort.TITLE_DESC.wireValue, UiMessages.app_sortNameZA()),
    DropdownChoice(F95Sort.RATING.wireValue, UiMessages.app_sortRating()),
    DropdownChoice(F95Sort.RATING_ASC.wireValue, UiMessages.app_sortRatingLowest())
)
