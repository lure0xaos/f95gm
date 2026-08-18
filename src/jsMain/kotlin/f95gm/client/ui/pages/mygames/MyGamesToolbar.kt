package f95gm.client.ui.pages.mygames

import dev.fritz2.core.*
import f95gm.client.actions.marks.editing.selectMarkedSort
import f95gm.client.actions.marks.editing.updateMarkedSearch
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.state.marks.marks
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.ui.shared.textFieldClass
import f95gm.domain.filters.F95Sort
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.myGamesToolbar() {
    div("games-search input-group flex-nowrap flex-grow-1 flex-shrink-1") {
        span("search-icon $secondaryNavigationButtonClass rounded-0 rounded-start") {
            attr("aria-hidden", "true")
            span("bi bi-search") {}
        }
        input("search-input $textFieldClass flex-grow-1") {
            type("search")
            placeholder(UiMessages.app_searchMarkedGamesPlaceholder())
            value(marks.data.map { it.search })
            inputs.values() handledBy { updateMarkedSearch(it) }
        }
        bootstrapDropdown(
            selected = marks.data.map { it.sort },
            selectedLabel = marks.data.map { state ->
                when (state.sort) {
                    F95Sort.DATE_ASC.wireValue -> UiMessages.app_sortDateOldest()
                    F95Sort.TITLE.wireValue -> UiMessages.app_sortName()
                    F95Sort.TITLE_DESC.wireValue -> UiMessages.app_sortNameZA()
                    else -> UiMessages.app_sortDate()
                }
            },
            options = flowOf(
                listOf(
                    DropdownChoice(F95Sort.DATE.wireValue, UiMessages.app_sortDate()),
                    DropdownChoice(F95Sort.DATE_ASC.wireValue, UiMessages.app_sortDateOldest()),
                    DropdownChoice(F95Sort.TITLE.wireValue, UiMessages.app_sortName()),
                    DropdownChoice(F95Sort.TITLE_DESC.wireValue, UiMessages.app_sortNameZA())
                )
            ),
            buttonClass = "$secondaryNavigationButtonClass rounded-0 rounded-end",
            widthClass = "w-auto"
        ) { selectMarkedSort(it) }
    }
}
