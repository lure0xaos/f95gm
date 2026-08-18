package f95gm.client.ui.pages.mygames

import dev.fritz2.core.RenderContext
import dev.fritz2.core.disabled
import dev.fritz2.core.type
import f95gm.client.actions.catalog.loading.loadFilterOptionsIfNeeded
import f95gm.client.actions.filters.loadSavedFiltersIfNeeded
import f95gm.client.actions.marks.loading.checkMarkedGames
import f95gm.client.actions.marks.loading.loadMarksIfNeeded
import f95gm.client.actions.marks.loading.retryMarkedGames
import f95gm.client.model.marks.markedGamesView
import f95gm.client.state.marks.marks
import f95gm.client.state.settings.appSettings
import f95gm.client.ui.navigation.responsiveNavbar
import f95gm.client.ui.shared.loading.pageLoadingOverlay
import f95gm.client.ui.shared.navigation.successOutlineNavigationButtonClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.myGamesPage() {
    div("marked-page") {
        responsiveNavbar(
            navigationId = "marked-navigation",
            catalogLink = true,
            toolbar = { myGamesToolbar() },
            leadingAccountAction = {
                button(successOutlineNavigationButtonClass) {
                    type("button")
                    span("bi bi-arrow-repeat me-1") {}
                    +UiMessages.app_checkForUpdates()
                    disabled(marks.data.map { it.checking })
                    clicks handledBy { checkMarkedGames(showToast = true) }
                }
            }
        )
        div("page-heading d-flex flex-wrap align-items-center justify-content-between gap-3 py-3") {
            div("page-title d-flex align-items-center gap-2") {
                span("bi bi-bookmark-star text-primary fs-4") {}
                h1("h4 mb-0") { +UiMessages.app_myGames() }
                span("badge text-bg-secondary") {
                    marks.data.map { it.items.size.toString() }.render { +it }
                }
            }
            marks.data.map { it.checking }.distinctUntilChanged().render {
                if (it) span("text-body-secondary small") { +UiMessages.app_checkingVersions() }
            }
        }
        div("page-status mt-1") {
            marks.data.map { it.error }.distinctUntilChanged().render { error ->
                if (error.isNotBlank()) {
                    div("feed-error alert alert-danger py-2 d-flex align-items-center justify-content-between gap-3") {
                        span { +error }
                        button("btn btn-sm btn-outline-danger") {
                            type("button")
                            +UiMessages.app_retry()
                            clicks handledBy { retryMarkedGames() }
                        }
                    }
                }
            }
        }
        myGamesPagination()
        div("feed w-100") {
            marks.data.map { state -> state.items.isEmpty() to markedGamesView(state).items }
                .distinctUntilChanged()
                .render { (hasNoMarks, items) ->
                    appSettings.data.map { it.cardSize.gridClass }.render { classNames ->
                        div("game-grid row $classNames g-4") {
                            if (items.isEmpty()) {
                                div("empty-column col-12") {
                                    div("empty-state card border-secondary-subtle text-center p-5") {
                                        span("empty-icon bi bi-search display-4 text-primary") {}
                                        h2("empty-title h4 mt-3") {
                                            +(if (hasNoMarks) UiMessages.app_noMarkedGames() else UiMessages.app_noMarkedGameMatches())
                                        }
                                        p("empty-hint text-body-secondary") {
                                            +(if (hasNoMarks) UiMessages.app_addMarkedGameHint() else UiMessages.app_noMarkedGameMatchesHint())
                                        }
                                    }
                                }
                            } else {
                                items.forEach { myGameCard(it) }
                            }
                        }
                    }
                }
            marks.data.map { it.loading }.distinctUntilChanged().render { loading ->
                if (loading) pageLoadingOverlay(UiMessages.app_loadingMarkedGames())
            }
        }
        myGamesPagination()
    }
    loadMarksIfNeeded()
    loadSavedFiltersIfNeeded()
    loadFilterOptionsIfNeeded()
}
