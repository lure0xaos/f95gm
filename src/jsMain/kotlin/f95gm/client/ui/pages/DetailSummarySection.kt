package f95gm.client.ui.pages

import dev.fritz2.core.*
import f95gm.client.model.detail.DetailState
import f95gm.client.state.config.ClientConstants
import f95gm.messages.UiMessages

/** Purely presentational detail summary; loading, retry, and body orchestration stay in the page. */
internal fun RenderContext.renderDetailSummary(state: DetailState) {
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
                        +state.title.value.take(ClientConstants.FIRST_CHAR_COUNT).uppercase()
                    }
                }
            }
        }
        div("summary-copy col-md-7") {
            p("summary-meta lead text-body-secondary") {
                +listOf(state.creator.value, state.date.value).filter { it.isNotBlank() }.joinToString(" · ")
            }
            div("summary-badges d-flex flex-wrap gap-2") {
                if (state.creator.value.isNotBlank()) span("creator-badge badge text-bg-secondary") {
                    +UiMessages.app_creator(state.creator.value)
                }
                if (state.date.value.isNotBlank()) span("date-badge badge text-bg-secondary") {
                    +UiMessages.app_started(state.date.value)
                }
            }
        }
    }
}
