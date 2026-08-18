package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.*
import f95gm.client.actions.catalog.filters.search.updateCatalogSearch
import f95gm.client.actions.catalog.filters.selection.selectCategory
import f95gm.client.actions.catalog.filters.selection.selectSort
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.state.catalog.catalog
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.navigationButtonClass
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.ui.shared.textFieldClass
import f95gm.domain.filters.F95Sort
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.catalogToolbarNavigation() {
    div("catalog-tools d-flex flex-wrap flex-xl-nowrap align-items-center gap-2 w-100") {
        val categoryChoices = listOf(
            DropdownChoice("games", UiMessages.app_games()),
            DropdownChoice("comics", UiMessages.app_comics()),
            DropdownChoice("animations", UiMessages.app_animations()),
            DropdownChoice("assets", UiMessages.app_assets()),
            DropdownChoice("mods", UiMessages.app_mods())
        )
        bootstrapDropdown(
            selected = catalog.data.map { it.category.wireValue },
            selectedLabel = catalog.data.map { state ->
                categoryChoices.firstOrNull { it.value == state.category.wireValue }?.label
                    ?: UiMessages.app_games()
            },
            options = flowOf(categoryChoices),
            buttonClass = navigationButtonClass,
            widthClass = "w-auto"
        ) { selectCategory(it) }
        div("catalog-search input-group flex-grow-1 flex-shrink-1") {
            span("search-icon $secondaryNavigationButtonClass rounded-0 rounded-start") {
                attr("aria-hidden", "true")
                span("bi bi-search") {}
            }
            input("search-input $textFieldClass flex-grow-1") {
                type("search")
                placeholder(UiMessages.app_searchTitlePlaceholder())
                value(catalog.data.map { it.search })
                inputs.values() handledBy { updateCatalogSearch(it) }
            }
            bootstrapDropdown(
                selected = catalog.data.map { it.sort.wireValue },
                selectedLabel = catalog.data.map { catalogSortLabel(it.sort) },
                options = flowOf(
                    listOf(
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
                ),
                buttonClass = "$secondaryNavigationButtonClass rounded-0 rounded-end",
                widthClass = "w-auto"
            ) { selectSort(it) }
        }
    }
}
