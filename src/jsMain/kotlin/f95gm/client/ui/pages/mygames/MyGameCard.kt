package f95gm.client.ui.pages.mygames

import dev.fritz2.core.RenderContext
import dev.fritz2.core.title
import dev.fritz2.core.type
import f95gm.client.actions.marks.mutations.saveMarkedTrackingState
import f95gm.client.actions.marks.mutations.unmarkGame
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.model.marks.MarkedGame
import f95gm.client.routing.navigation.myGamesDetailRoute
import f95gm.client.routing.navigation.router
import f95gm.client.state.catalog.catalogOptions
import f95gm.client.state.options.localizedTrackingState
import f95gm.client.state.options.trackingStateOptions
import f95gm.client.ui.shared.cards.GameCardData
import f95gm.client.ui.shared.cards.gameCardContent
import f95gm.client.ui.shared.content.resolveGamePrefixes
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.dangerNavigationButtonClass
import f95gm.client.ui.shared.navigation.primaryNavigationButtonClass
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.myGameCard(mark: MarkedGame) {
    div("game-column col d-flex") {
        div("game-card card h-100 w-100 d-flex flex-column shadow-sm overflow-hidden") {
            attr("style", "cursor: pointer;")
            a("game-link text-decoration-none text-reset flex-grow-1") {
                clicks.map { myGamesDetailRoute(mark.threadId.value) } handledBy router.navTo
                catalogOptions.data.render { options ->
                    gameCardContent(
                        card = GameCardData(
                            mark.title.value,
                            mark.developer.value,
                            mark.cover.value,
                            resolveGamePrefixes(mark.prefixes, options.prefixes),
                            mark.version.value,
                            mark.date.value,
                            mark.likes.value,
                            mark.views.value,
                            mark.rating,
                            mark.isNew
                        ),
                        badges = {
                            span("badge text-bg-primary") { +localizedTrackingState(mark.trackingState).uppercase() }
                            if (mark.updateAvailable) span("badge text-bg-warning") { +UiMessages.app_update() }
                        }
                    )
                }
            }
            div("game-actions card-footer d-flex align-items-center gap-2") {
                bootstrapDropdown(
                    selected = flowOf(mark.trackingState.wireValue),
                    selectedLabel = flowOf(
                        trackingStateOptions.firstOrNull { it.first.wireValue == mark.trackingState.wireValue }?.second
                            ?: UiMessages.app_unmark()
                    ),
                    options = flowOf(trackingStateOptions.map {
                        DropdownChoice(
                            it.first.wireValue,
                            it.second
                        )
                    } + DropdownChoice("", UiMessages.app_unmark())),
                    buttonClass = "$secondaryNavigationButtonClass text-start flex-grow-1"
                ) { selected ->
                    if (selected.isBlank()) unmarkGame(mark.threadId.value) else saveMarkedTrackingState(mark, selected)
                }
                if (mark.updateAvailable) {
                    button(primaryNavigationButtonClass) {
                        type("button")
                        +UiMessages.app_downloaded(mark.latestVersion.value)
                        clicks handledBy { saveMarkedTrackingState(mark, "downloaded", acknowledge = true) }
                    }
                }
                button(dangerNavigationButtonClass) {
                    type("button")
                    span("bi bi-trash") {}
                    attr("aria-label", UiMessages.app_remove())
                    title(UiMessages.app_remove())
                    clicks handledBy { unmarkGame(mark.threadId.value) }
                }
            }
        }
    }
}
