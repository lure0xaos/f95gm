package f95gm.client.ui.pages.mygames

import dev.fritz2.core.*
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.state.runtime.ClientMarksUiPort
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.ui.shared.textFieldClass
import f95gm.domain.filters.F95Sort
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.myGamesToolbar(runtime: ClientMarksUiPort) {
    val marks = runtime.marksView
    div("games-search input-group flex-nowrap flex-grow-1 flex-shrink-1") {
        span("search-icon $secondaryNavigationButtonClass rounded-0 rounded-start") {
            attr("aria-hidden", "true")
            span("bi bi-search") {}
        }
        input("search-input $textFieldClass flex-grow-1") {
            type("search")
            placeholder(UiMessages.app_searchMarkedGamesPlaceholder())
            value(marks.data.map { it.search.value })
            inputs.values() handledBy { runtime.marksCommands.updateMarkedSearch(it) }
        }
        bootstrapDropdown(
            runtime = runtime,
            selected = marks.data.map { it.sort },
            selectedLabel = marks.data.map { state -> markedSortLabel(state.sort) },
            options = flowOf(
                listOf(
                    DropdownChoice(F95Sort.DATE, UiMessages.app_sortDate()),
                    DropdownChoice(F95Sort.DATE_ASC, UiMessages.app_sortDateOldest()),
                    DropdownChoice(F95Sort.TITLE, UiMessages.app_sortName()),
                    DropdownChoice(F95Sort.TITLE_DESC, UiMessages.app_sortNameZA())
                )
            ),
            buttonClass = "$secondaryNavigationButtonClass rounded-0 rounded-end",
            widthClass = "w-auto"
        ) { runtime.marksCommands.selectMarkedSort(it) }
    }
}
