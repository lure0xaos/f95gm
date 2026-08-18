package f95gm.client.ui.pages

import dev.fritz2.core.RenderContext
import dev.fritz2.core.alt
import dev.fritz2.core.src
import dev.fritz2.core.type
import f95gm.client.actions.detail.loadDetailIfNeeded
import f95gm.client.actions.detail.retryDetail
import f95gm.client.actions.filters.loadSavedFiltersIfNeeded
import f95gm.client.model.detail.DetailBodyState
import f95gm.client.routing.navigation.Route
import f95gm.client.routing.navigation.router
import f95gm.client.state.config.ClientConstants
import f95gm.client.state.detail.detail
import f95gm.client.ui.detail.metadata.detailBadgeMeta
import f95gm.client.ui.detail.metadata.detailGenres
import f95gm.client.ui.detail.metadata.detailMarkControls
import f95gm.client.ui.detail.metadata.detailMetaCards
import f95gm.client.ui.detail.renderDetailBody
import f95gm.client.ui.navigation.*
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.detailPage(threadId: String, backRoute: Route) {
    div("detail-page container-xl") {
        div("detail-nav navbar border-bottom py-3") {
            navbarBrand()
            jvmConnectionDetails()
            navbarNavigationLink(NavbarLink.TRACKED_GAMES)
            navbarAccountLinks()
        }
        div("detail-actions d-flex justify-content-between align-items-center gap-3 mt-4") {
            a("back-link $secondaryNavigationButtonClass") {
                clicks.map { backRoute } handledBy router.navTo
                span("bi bi-arrow-left me-1") {}
                +UiMessages.app_back()
            }
            a("external-link $secondaryNavigationButtonClass") {
                attr("href", "https://f95zone.to/threads/$threadId/")
                attr("target", "_blank")
                attr("rel", "noopener noreferrer")
                span("bi bi-box-arrow-up-right me-1") {}
                +UiMessages.app_openF95zoneThread()
            }
        }
        div("detail-content mt-5") {
            detail.data.render { state ->
                when {
                    state.loading -> div("detail-loading alert alert-info py-2") { +UiMessages.app_loadingDetails() }
                    state.error.isNotBlank() -> div("detail-error alert alert-danger py-2 d-flex align-items-center justify-content-between gap-3") {
                        span { +state.error }
                        button("btn btn-sm btn-outline-danger") {
                            type("button")
                            +UiMessages.app_retry()
                            clicks handledBy { retryDetail(threadId) }
                        }
                    }
                    else -> {
                        h1("detail-title display-5 fw-bold mb-0") { +state.title.value }
                        detailMarkControls(state)
                        div("detail-summary row g-4 align-items-start mt-3") {
                            div("cover-column col-md-5") {
                                div("cover-card card border-0 shadow overflow-hidden") {
                                    if (state.cover.value.isNotBlank()) {
                                        img("detail-cover cover-image card-img-top object-fit-cover") {
                                            alt(state.title.value)
                                            src(state.cover.value)
                                            attr("loading", "lazy")
                                            attr("decoding", "async")
                                        }
                                    } else {
                                        div("cover-fallback detail-cover-fallback bg-body-secondary d-flex align-items-center justify-content-center display-1 fw-bold text-primary") {
                                            +state.title.value.take(
                                                ClientConstants.FIRST_CHAR_COUNT
                                            ).uppercase()
                                        }
                                    }
                                }
                            }
                            div("summary-copy col-md-7") {
                                p("summary-meta lead text-body-secondary") {
                                    +listOf(state.creator.value, state.date.value).filter { it.isNotBlank() }
                                        .joinToString(" · ")
                                }
                                div("summary-badges d-flex flex-wrap gap-2") {
                                    if (state.creator.value.isNotBlank()) span("creator-badge badge text-bg-secondary") {
                                        +UiMessages.app_creator(
                                            state.creator.value
                                        )
                                    }
                                    if (state.date.value.isNotBlank()) span("date-badge badge text-bg-secondary") {
                                        +UiMessages.app_started(
                                            state.date.value
                                        )
                                    }
                                }
                            }
                        }
                        val genres = detailGenres(state)
                        val badgeMeta = detailBadgeMeta(state)
                        if (genres.isNotEmpty() || badgeMeta.isNotEmpty()) {
                            div("detail-badges d-flex flex-wrap gap-2 mt-3") {
                                genres.forEach { genre ->
                                    span("genre-badge badge text-bg-info") {
                                        span("bi bi-tag me-1") {}
                                        +genre
                                    }
                                }
                                badgeMeta.forEach { meta ->
                                    span("meta-badge badge text-bg-secondary") {
                                        span("text-uppercase me-1") { +"${meta.label.value}:" }
                                        +meta.value.value
                                    }
                                }
                            }
                        }
                        val metadata = detailMetaCards(state)
                        if (metadata.isNotEmpty()) {
                            div("meta-grid row row-cols-2 row-cols-sm-3 row-cols-md-4 row-cols-lg-6 g-2 mt-4") {
                                metadata.forEach { meta ->
                                    div("meta-column col") {
                                        div("meta-card card h-100 p-2") {
                                            span("meta-label small text-uppercase text-body-secondary fw-semibold") { +meta.label.value }
                                            span(
                                                "meta-value small mt-1 ${
                                                    if (meta.label.value.equals(
                                                            UiMessages.app_genre(),
                                                            ignoreCase = true
                                                        )
                                                    ) "text-wrap" else "text-truncate"
                                                }"
                                            ) {
                                                +meta.value.value
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        div("body-section mt-4 mb-5") {
                            div("detail-body card-body p-3 p-md-4") {
                                detail.data.map { detailState ->
                                    DetailBodyState(
                                        detailState.overviewHtml.value,
                                        detailState.sections,
                                        detailState.bodyHtml.value
                                    )
                                }.distinctUntilChanged().render { body ->
                                    renderDetailBody(body.overviewHtml, body.sections, body.fallbackHtml)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    loadDetailIfNeeded(threadId)
    loadSavedFiltersIfNeeded()
}
