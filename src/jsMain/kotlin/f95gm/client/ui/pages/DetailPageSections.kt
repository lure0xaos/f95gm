package f95gm.client.ui.pages

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.model.detail.DetailBodyState
import f95gm.client.routing.navigation.TypedRoute
import f95gm.client.routing.navigation.router
import f95gm.client.model.detail.loading
import f95gm.client.model.detail.error
import f95gm.client.ui.detail.metadata.detailBadgeMeta
import f95gm.client.ui.detail.metadata.detailGenres
import f95gm.client.ui.detail.metadata.detailMarkControls
import f95gm.client.ui.detail.metadata.detailMetaCards
import f95gm.client.ui.detail.renderDetailBody
import f95gm.client.ui.navigation.*
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.state.runtime.ClientDetailUiPort
import f95gm.domain.values.F95ThreadId
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.renderDetailPageSections(runtime: ClientDetailUiPort, threadId: F95ThreadId) {
    val detail = runtime.detailView
        div("detail-content mt-5") {
            detail.data.render { state ->
                when {
                    state.loading -> div("detail-loading alert alert-info py-2") { +UiMessages.app_loadingDetails() }
                    state.error.isNotBlank() -> div("detail-error alert alert-danger py-2 d-flex align-items-center justify-content-between gap-3") {
                        span { +state.error }
                        button("btn btn-sm btn-outline-danger") {
                            type("button")
                            +UiMessages.app_retry()
                            clicks handledBy { runtime.detailCommands.retryDetail(threadId) }
                        }
                    }
                    else -> {
                        h1("detail-title display-5 fw-bold mb-0") { +state.title.value }
                        detailMarkControls(runtime, state)
                        renderDetailSummary(state)
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
                                    renderDetailBody(runtime, body.overviewHtml, body.sections, body.fallbackHtml)
                                }
                            }
                        }
                    }
                }
            }
        }
}

