package f95gm.client.ui.shared.cards

import dev.fritz2.core.*
import f95gm.client.actions.images.imageLoadFailed
import f95gm.client.actions.images.retryImage
import f95gm.client.data.presentation.formatRating
import f95gm.client.state.config.ClientConstants
import f95gm.client.state.images.imageRetry
import f95gm.client.state.settings.appSettings
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.gameCardContent(
    card: GameCardData,
    badges: RenderContext.() -> Unit = {}
) {
    div("game-cover position-relative ratio ratio-16x9 overflow-hidden rounded-top bg-body-secondary") {
        if (card.cover.isNotBlank()) {
            imageRetry.data
                .map { it.failedSources.contains(card.cover) to (it.attempts[card.cover] ?: 0) }
                .distinctUntilChanged()
                .render { (failed, attempt) ->
                    if (failed) {
                        div("cover-fallback d-flex flex-column align-items-center justify-content-center h-100 w-100 text-primary gap-2") {
                            span("display-4 fw-bold") { +card.title.take(ClientConstants.FIRST_CHAR_COUNT).uppercase() }
                            button("$secondaryNavigationButtonClass btn-sm") {
                                type("button")
                                span("bi bi-arrow-repeat me-1") {}
                                +UiMessages.app_retry()
                                clicks {
                                    stopPropagation()
                                    preventDefault()
                                } handledBy { retryImage(card.cover) }
                            }
                        }
                    } else {
                        img("img-fluid w-100 h-100 object-fit-cover position-absolute top-0 start-0") {
                            alt(card.title)
                            src(
                                if (attempt == 0) card.cover
                                else card.cover + if ('?' in card.cover) "&retry=$attempt" else "?retry=$attempt"
                            )
                            attr("width", ClientConstants.GAME_COVER_WIDTH.toString())
                            attr("height", ClientConstants.GAME_COVER_HEIGHT.toString())
                            attr("loading", appSettings.data.map { if (it.lazyLoadImages) "lazy" else "eager" })
                            attr("decoding", "async")
                            sizes("(min-width: 1400px) 25vw, (min-width: 992px) 33vw, (min-width: 576px) 50vw, 100vw")
                            errors handledBy { imageLoadFailed(card.cover) }
                        }
                    }
                }
        } else {
            div("cover-fallback d-flex align-items-center justify-content-center h-100 w-100 text-primary display-4 fw-bold") {
                +card.title.take(ClientConstants.FIRST_CHAR_COUNT).uppercase()
            }
        }
        div("game-badges position-absolute top-0 start-0 d-flex flex-wrap align-items-start gap-1 p-2 w-100") {
            if (card.isNew) span("game-badge badge text-bg-success") { +UiMessages.app_new() }
            if (card.version != UiMessages.app_versionUnknown()) {
                span("game-version game-badge badge text-bg-secondary") { +card.version }
            }
            div("game-status d-flex flex-wrap gap-1") { badges() }
            div("game-labels d-flex flex-wrap justify-content-end gap-1 ms-auto") {
                span("game-kind badge text-bg-secondary") { +UiMessages.app_game() }
                card.prefixes.forEach { prefix ->
                    span("game-prefix badge text-bg-info") { +prefix }
                }
            }
        }
    }
    div("game-body card-body d-flex flex-column") {
        h2("game-title h5 card-title text-truncate mt-2") { +card.title }
        p("game-developer card-text text-body-secondary text-truncate") {
            +card.developer.ifBlank { UiMessages.app_communityRelease() }
        }
        div("game-stats d-flex gap-3 border-top pt-3 mt-auto small text-body-secondary") {
            span("game-date game-stat") { span("bi bi-clock me-1") {}; +card.date }
            span("game-stat") { span("bi bi-heart me-1") {}; +card.likes }
            span("game-stat") { span("bi bi-eye me-1") {}; +card.views }
            span("game-rating game-stat ms-auto text-warning") { span("bi bi-star-fill me-1") {}; +formatRating(card.rating) }
        }
    }
}
