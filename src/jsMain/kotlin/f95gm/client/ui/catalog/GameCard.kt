package f95gm.client.ui.catalog

import dev.fritz2.core.RenderContext
import f95gm.client.model.catalog.Game
import f95gm.client.routing.navigation.latestUpdatesDetailRoute
import f95gm.client.routing.navigation.router
import f95gm.client.state.catalog.catalogOptions
import f95gm.client.state.marks.marks
import f95gm.client.state.options.localizedTrackingState
import f95gm.client.ui.shared.cards.GameCardData
import f95gm.client.ui.shared.cards.gameCardContent
import f95gm.client.ui.shared.content.resolveGamePrefixes
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.gameCard(game: Game) {
    a("game-link col d-flex text-decoration-none text-reset") {
        attr("style", "cursor: pointer;")
        clicks.map { latestUpdatesDetailRoute(game.threadId.value) } handledBy router.navTo
        div("game-card card h-100 w-100 d-flex flex-column shadow-sm overflow-hidden") {
            catalogOptions.data.render { options ->
                gameCardContent(
                    card = GameCardData(
                        game.title.value,
                        game.developer.value,
                        game.cover.value,
                        resolveGamePrefixes(game.prefixes, options.prefixes),
                        game.version.value,
                        game.date.value,
                        game.likes.value,
                        game.views.value,
                        game.rating,
                        game.isNew
                    ),
                    badges = {
                        marks.data.map { it.items.firstOrNull { mark -> mark.threadId == game.threadId } }
                            .distinctUntilChanged()
                            .render { mark ->
                                if (mark != null) {
                                    span("badge text-bg-primary") { +localizedTrackingState(mark.trackingState).uppercase() }
                                    if (mark.updateAvailable) span("badge text-bg-warning") { +UiMessages.app_update() }
                                }
                            }
                    }
                )
            }
        }
    }
}
